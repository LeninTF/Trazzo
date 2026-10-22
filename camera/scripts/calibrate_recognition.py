"""Calibra el umbral de reconocimiento facial con datos, no a ojo.

    .venv\\Scripts\\python scripts\\calibrate_recognition.py --pack trazzo_r50
    .venv\\Scripts\\python scripts\\calibrate_recognition.py --pack buffalo_sc

Usa LFW (Labeled Faces in the Wild, University of Massachusetts), el benchmark público estándar
de verificación facial. Una sola persona frente a la cámara no permite medir cuánto se parecen
personas DISTINTAS; LFW aporta miles de identidades.

Métricas (terminología ISO/IEC 19795-1):
  FMR   false match rate: pares de personas distintas que superan el umbral.
  FNMR  false non-match rate: pares de la misma persona que no lo superan.
  1:N   simulación con la regla real del servicio (umbral + margen entre 1.º y 2.º):
        TPIR  persona registrada identificada correctamente
        MIS   persona registrada identificada como OTRA (el error que no debe ocurrir)
        FPIR  persona NO registrada aceptada como alguien de la galería

Es conservador: cada plantilla sale de UNA foto de internet (el servicio promedia varios frames
en vivo con control de calidad), así que en uso real la FNMR es menor. La FMR, que es la que
define la seguridad, sí se traslada.

Se ejecuta con prioridad baja (proceso de fondo) y guarda los embeddings en eval/ para no
recalcular. eval/ está en .gitignore.
"""
import argparse
import ctypes
import json
import os
import random
import sys
import tarfile
import time
import urllib.request

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
EVAL_DIR = os.path.join(BASE_DIR, "eval")
LFW_URL = "https://ndownloader.figshare.com/files/5976015"
LFW_TGZ = os.path.join(EVAL_DIR, "lfw-funneled.tgz")
LFW_DIR = os.path.join(EVAL_DIR, "lfw_funneled")
LFW_BYTES = 243_346_528

FMR_TARGETS = (1e-3, 1e-4, 1e-5)
# FMR por comparación con la que se elige el umbral. En 1:N la tasa de aceptar a un
# desconocido crece ~ N x FMR: con 1e-5 y 200 personas registradas, ~0.2 %.
RECOMMENDED_FMR = 1e-5


def _below_normal_priority() -> None:
    if sys.platform == "win32":
        BELOW_NORMAL_PRIORITY_CLASS = 0x4000
        k32 = ctypes.WinDLL("kernel32")
        k32.GetCurrentProcess.restype = ctypes.c_void_p
        k32.SetPriorityClass(ctypes.c_void_p(k32.GetCurrentProcess()), BELOW_NORMAL_PRIORITY_CLASS)


def _ensure_lfw() -> None:
    os.makedirs(EVAL_DIR, exist_ok=True)
    if os.path.isdir(LFW_DIR):
        return
    if not (os.path.isfile(LFW_TGZ) and os.path.getsize(LFW_TGZ) == LFW_BYTES):
        print(f"descargando LFW ({LFW_BYTES // 2**20} MB)...")
        tmp = LFW_TGZ + ".part"
        req = urllib.request.Request(LFW_URL, headers={"User-Agent": "Mozilla/5.0"})
        with urllib.request.urlopen(req, timeout=60) as r, open(tmp, "wb") as out:
            done = 0
            while chunk := r.read(1 << 20):
                out.write(chunk)
                done += len(chunk)
                sys.stdout.write(f"\r  {done * 100 // LFW_BYTES}%")
                sys.stdout.flush()
        print()
        if os.path.getsize(tmp) != LFW_BYTES:
            raise SystemExit("ERROR: descarga de LFW incompleta")
        os.replace(tmp, LFW_TGZ)
    print("extrayendo LFW...")
    with tarfile.open(LFW_TGZ) as tf:
        members = [m for m in tf.getmembers()
                   if m.isfile() and m.name.endswith(".jpg") and ".." not in m.name]
        tf.extractall(EVAL_DIR, members=members)


def _select_images(max_images: int, max_per_person: int, seed: int) -> list[tuple[str, str]]:
    people = []
    for name in sorted(os.listdir(LFW_DIR)):
        folder = os.path.join(LFW_DIR, name)
        if os.path.isdir(folder):
            files = sorted(f for f in os.listdir(folder) if f.endswith(".jpg"))
            if len(files) >= 2:
                people.append((name, [os.path.join(folder, f) for f in files]))
    random.Random(seed).shuffle(people)
    selected = []
    for name, files in people:
        for path in files[:max_per_person]:
            selected.append((name, path))
        if len(selected) >= max_images:
            break
    return selected


def _embed(selected, cache_path):
    import numpy as np
    if os.path.isfile(cache_path):
        data = np.load(cache_path, allow_pickle=False)
        print(f"usando embeddings en cache: {cache_path}")
        return list(data["names"]), data["embeddings"], data["quality_ok"]

    import cv2
    from infrastructure.face_engine import FaceEngine
    from services import face_quality

    engine = FaceEngine.instance()
    names, embeddings, quality_ok = [], [], []
    t0 = time.monotonic()
    for i, (name, path) in enumerate(selected, 1):
        img = cv2.imread(path)
        faces = engine.detect(img) if img is not None else []
        if faces:
            h, w = img.shape[:2]
            # LFW centra a la persona retratada; puede haber otras caras al fondo
            face = min(faces, key=lambda f: abs((f.bbox[0] + f.bbox[2]) / 2 - w / 2)
                       + abs((f.bbox[1] + f.bbox[3]) / 2 - h / 2))
            emb = face.embedding / max(1e-6, float(np.linalg.norm(face.embedding)))
            names.append(name)
            embeddings.append(emb.astype(np.float32))
            quality_ok.append(face_quality.assess(img, face).ok)
        if i % 100 == 0 or i == len(selected):
            rate = i / (time.monotonic() - t0)
            sys.stdout.write(f"\r  embeddings {i}/{len(selected)} ({rate:.1f} img/s, "
                             f"faltan ~{(len(selected) - i) / rate / 60:.1f} min)")
            sys.stdout.flush()
    print()
    embeddings = np.stack(embeddings)
    np.savez_compressed(cache_path, names=np.array(names), embeddings=embeddings,
                        quality_ok=np.array(quality_ok))
    return names, embeddings, np.array(quality_ok)


def _verification_metrics(labels, emb, current_threshold):
    import numpy as np
    sims = emb @ emb.T
    iu = np.triu_indices(len(labels), k=1)
    same = labels[iu[0]] == labels[iu[1]]
    scores = sims[iu]
    genuine, impostor = scores[same], scores[~same]
    out = {
        "genuine_pairs": int(genuine.size),
        "impostor_pairs": int(impostor.size),
        "genuine_p5_p50": [round(float(np.percentile(genuine, 5)), 3), round(float(np.median(genuine)), 3)],
        "impostor_p50_max": [round(float(np.median(impostor)), 3), round(float(impostor.max()), 3)],
        "at_fmr": [],
        "current": {
            "threshold": current_threshold,
            "fmr": float(np.mean(impostor >= current_threshold)),
            "fnmr": float(np.mean(genuine < current_threshold)),
        },
    }
    for fmr in FMR_TARGETS:
        t = float(np.quantile(impostor, 1 - fmr))
        out["at_fmr"].append({"fmr": fmr, "threshold": round(t, 3),
                              "fnmr": round(float(np.mean(genuine < t)), 4)})
    return out


def _identification_metrics(labels, emb, threshold, margin, seed):
    """Galería: la mitad de las personas (1 plantilla cada una). Sondas: el resto de sus fotos
    (registradas) y todas las fotos de la otra mitad (no registradas)."""
    import numpy as np
    persons = sorted(set(labels.tolist()))
    random.Random(seed).shuffle(persons)
    enrolled = set(persons[: len(persons) // 2])

    gallery_idx, gallery_label, probe_known, probe_unknown = [], [], [], []
    seen = set()
    for i, lab in enumerate(labels.tolist()):
        if lab in enrolled:
            if lab not in seen:
                seen.add(lab)
                gallery_idx.append(i)
                gallery_label.append(lab)
            else:
                probe_known.append(i)
        else:
            probe_unknown.append(i)
    gallery = emb[gallery_idx]
    gallery_label = np.array(gallery_label)

    def decide(idx):
        s = emb[idx] @ gallery.T
        order = np.argsort(-s, axis=1)
        best = s[np.arange(len(idx)), order[:, 0]]
        second = s[np.arange(len(idx)), order[:, 1]]
        accepted = (best >= threshold) & ((best - second) >= margin)
        return accepted, gallery_label[order[:, 0]]

    acc_k, who_k = decide(np.array(probe_known))
    true_k = labels[probe_known]
    acc_u, _ = decide(np.array(probe_unknown))
    return {
        "gallery_size": len(gallery_idx),
        "known_probes": len(probe_known),
        "unknown_probes": len(probe_unknown),
        "tpir": round(float(np.mean(acc_k & (who_k == true_k))), 4),
        "misidentification": round(float(np.mean(acc_k & (who_k != true_k))), 4),
        "fpir": round(float(np.mean(acc_u)), 4),
    }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--pack", default="trazzo_r50")
    parser.add_argument("--max-images", type=int, default=3000)
    parser.add_argument("--max-per-person", type=int, default=4)
    parser.add_argument("--seed", type=int, default=7)
    args = parser.parse_args()

    # antes de importar config: el pack y los hilos se leen al importar
    os.environ["MODEL_PACK"] = args.pack
    os.environ.setdefault("ORT_INTRA_THREADS", "2")
    sys.path.insert(0, os.path.join(BASE_DIR, "src"))
    _below_normal_priority()

    import numpy as np
    from config import SETTINGS

    _ensure_lfw()
    selected = _select_images(args.max_images, args.max_per_person, args.seed)
    cache = os.path.join(EVAL_DIR, f"embeddings_{args.pack}_{args.max_images}_{args.seed}.npz")
    names, emb, quality_ok = _embed(selected, cache)

    # como en producción: solo rostros que pasan el control de calidad
    keep = np.asarray(quality_ok, dtype=bool)
    labels = np.array(names)[keep]
    emb = emb[keep]
    counts = {n: int(c) for n, c in zip(*np.unique(labels, return_counts=True))}
    multi = np.array([counts[n] >= 2 for n in labels])
    labels, emb = labels[multi], emb[multi]
    print(f"pack={args.pack}: {len(labels)} rostros de {len(set(labels.tolist()))} personas "
          f"(descartados por calidad: {int((~keep).sum())})")

    verification = _verification_metrics(labels, emb, SETTINGS.recognition_threshold)
    recommended = next(r["threshold"] for r in verification["at_fmr"] if r["fmr"] == RECOMMENDED_FMR)
    recommended = float(np.ceil(recommended * 100) / 100)

    identification = []
    for t in sorted({SETTINGS.recognition_threshold, recommended,
                     round(recommended - 0.05, 2), round(recommended + 0.05, 2)}):
        for m in (0.0, SETTINGS.recognition_margin):
            r = _identification_metrics(labels, emb, t, m, args.seed)
            identification.append({"threshold": t, "margin": m, **r})

    report = {"pack": args.pack, "recommended_threshold": recommended,
              "recommended_fmr": RECOMMENDED_FMR, "verification": verification,
              "identification": identification}
    path = os.path.join(EVAL_DIR, f"calibration_{args.pack}.json")
    with open(path, "w", encoding="utf-8") as fh:
        json.dump(report, fh, indent=2)

    v = verification
    print(f"\nverificacion 1:1  ({v['genuine_pairs']} pares misma persona, {v['impostor_pairs']} pares distintas)")
    print(f"  misma persona  p5={v['genuine_p5_p50'][0]}  mediana={v['genuine_p5_p50'][1]}")
    print(f"  distintas      mediana={v['impostor_p50_max'][0]}  maximo={v['impostor_p50_max'][1]}")
    for r in v["at_fmr"]:
        print(f"  FMR={r['fmr']:.0e} -> umbral {r['threshold']:.3f}  FNMR={r['fnmr']:.2%}")
    c = v["current"]
    print(f"  umbral actual {c['threshold']}: FMR={c['fmr']:.2e}  FNMR={c['fnmr']:.2%}")
    print("\nidentificacion 1:N (galeria de la mitad de las personas)")
    for r in identification:
        print(f"  umbral={r['threshold']:.2f} margen={r['margin']:.2f}  TPIR={r['tpir']:.2%}  "
              f"MIS={r['misidentification']:.2%}  FPIR={r['fpir']:.2%}  "
              f"(galeria={r['gallery_size']}, sondas={r['known_probes']}+{r['unknown_probes']})")
    print(f"\numbral recomendado (FMR {RECOMMENDED_FMR:.0e}): {recommended:.2f}   reporte: {path}")


if __name__ == "__main__":
    main()
