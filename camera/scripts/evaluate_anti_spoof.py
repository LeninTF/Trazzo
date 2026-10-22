"""Evalúa el anti-spoof con ataques reales frente a la cámara (ISO/IEC 30107-3).

    .venv\\Scripts\\python scripts\\evaluate_anti_spoof.py

Teclas en la ventana (cada grabación dura GRABACION_S segundos):
    R  persona REAL frente a la cámara (moverse natural, distintas distancias)
    1  ataque: foto en la pantalla de un celular
    2  ataque: foto impresa en papel
    3  ataque: video en pantalla (celular o laptop)
    Q  terminar y ver el reporte

Métricas:
    BPCER  frames de persona real rechazados (molestia para el usuario)
    APCER  frames de ataque aceptados como reales (riesgo de seguridad)
    Además se simula la captura pasiva real del servicio (8 frames aceptados antes de 30
    rechazos seguidos) para saber si un ataque lograría pasar la etapa del CNN.

Solo se guardan puntajes (sin imágenes) en eval/anti_spoof_<fecha>.json.
Grabar al menos 2 veces REAL y 2 veces cada ataque disponible, con la luz habitual del lugar.
"""
import json
import os
import sys
import time

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(BASE_DIR, "src"))

import cv2  # noqa: E402
import numpy as np  # noqa: E402

from config import SETTINGS  # noqa: E402
from infrastructure.camera import CameraCapture  # noqa: E402
from infrastructure.face_engine import FaceEngine  # noqa: E402
from infrastructure.process_qos import disable_execution_speed_throttling  # noqa: E402
from services import face_quality  # noqa: E402
from services.anti_spoof_service import AntiSpoofingService  # noqa: E402

GRABACION_S = 8.0
WINDOW = "Evaluacion anti-spoof"
KINDS = {
    ord("r"): ("real", "PERSONA REAL"),
    ord("1"): ("celular_foto", "ATAQUE: foto en celular"),
    ord("2"): ("foto_impresa", "ATAQUE: foto impresa"),
    ord("3"): ("video_pantalla", "ATAQUE: video en pantalla"),
}
THRESHOLDS = [round(t, 2) for t in np.arange(0.30, 0.95, 0.05)]

# réplica de las constantes de services/challenge_service.run_passive
_PASSIVE_TARGET_FRAMES = 8
_MAX_SPOOF_STREAK = 30


def capture_passes(scores: list[float], threshold: float) -> float:
    """Fracción de puntos de inicio desde los que la captura pasiva completaría la etapa CNN:
    8 frames aceptados sin 30 rechazos seguidos Y mediana de todos los evaluados >= umbral."""
    if len(scores) < _PASSIVE_TARGET_FRAMES:
        return 0.0
    starts = range(0, len(scores) - _PASSIVE_TARGET_FRAMES + 1)
    passed = 0
    for s in starts:
        accepted = streak = 0
        for i, score in enumerate(scores[s:], 1):
            if score >= threshold:
                accepted += 1
                streak = 0
                if accepted >= _PASSIVE_TARGET_FRAMES:
                    if np.median(scores[s:s + i]) >= threshold:
                        passed += 1
                    break
            else:
                streak += 1
                if streak >= _MAX_SPOOF_STREAK:
                    break
    return passed / len(starts)


def report(recordings: list[dict]) -> dict:
    real = [r["scores"] for r in recordings if r["kind"] == "real"]
    attacks = [r for r in recordings if r["kind"] != "real"]
    real_frames = np.concatenate([np.array(s) for s in real]) if real else np.array([])
    attack_frames = (np.concatenate([np.array(r["scores"]) for r in attacks])
                     if attacks else np.array([]))
    rows = []
    for t in THRESHOLDS:
        row = {"threshold": t}
        if real_frames.size:
            row["bpcer_frame"] = float(np.mean(real_frames < t))
            row["real_capture_pass"] = float(np.mean([capture_passes(s, t) for s in real]))
        if attack_frames.size:
            row["apcer_frame"] = float(np.mean(attack_frames >= t))
            row["attack_capture_pass"] = float(max(capture_passes(r["scores"], t) for r in attacks))
        rows.append(row)

    recommended = None
    if real_frames.size and attack_frames.size:
        # el umbral más bajo (más cómodo para la persona real) con el que ningún ataque grabado
        # completa la captura y como máximo 1 % de frames de ataque pasan
        safe = [r for r in rows if r["attack_capture_pass"] == 0.0 and r["apcer_frame"] <= 0.01]
        if safe:
            recommended = min(safe, key=lambda r: r["threshold"])["threshold"]
    return {"current_threshold": SETTINGS.anti_spoof_min_real, "recommended_threshold": recommended,
            "real_frames": int(real_frames.size), "attack_frames": int(attack_frames.size),
            "by_threshold": rows}


def print_report(rep: dict, recordings: list[dict]) -> None:
    print("\ngrabaciones:")
    for r in recordings:
        s = np.array(r["scores"])
        print(f"  {r['kind']:15s} frames={s.size:3d}  mediana={np.median(s):.2f}  p10={np.percentile(s, 10):.2f}  "
              f"p90={np.percentile(s, 90):.2f}  poca_luz={r['poor_light_ratio']:.0%}")
    print(f"\n{'umbral':>6}  {'BPCER':>7}  {'real pasa':>9}  {'APCER':>7}  {'ataque pasa':>11}")
    for row in rep["by_threshold"]:
        mark = " <- actual" if abs(row["threshold"] - rep["current_threshold"]) < 1e-6 else ""
        print(f"{row['threshold']:6.2f}  {row.get('bpcer_frame', float('nan')):7.1%}  "
              f"{row.get('real_capture_pass', float('nan')):9.0%}  {row.get('apcer_frame', float('nan')):7.1%}  "
              f"{row.get('attack_capture_pass', float('nan')):11.0%}{mark}")
    if rep["recommended_threshold"] is None:
        if rep["attack_frames"] and rep["real_frames"]:
            print("\nNINGUN umbral bloquea todos los ataques grabados sin rechazar a la persona real: "
                  "el CNN no basta en estas condiciones (mejorar luz o usar camara IR).")
        else:
            print("\nfaltan grabaciones: se necesita al menos una REAL y un ataque")
    else:
        print(f"\numbral recomendado: {rep['recommended_threshold']:.2f} "
              f"(actual {rep['current_threshold']:.2f}; se configura con ANTI_SPOOF_MIN_REAL)")


def main() -> None:
    disable_execution_speed_throttling()
    engine = FaceEngine.instance()
    anti_spoof = AntiSpoofingService.instance()
    if not anti_spoof.enabled:
        raise SystemExit("modelos anti-spoof no cargados: corre scripts/download_anti_spoof.py")
    cam = CameraCapture()
    recordings: list[dict] = []
    active = None  # (kind, label, fin, scores, poor)
    last_status = "R real | 1 foto celular | 2 foto impresa | 3 video | Q terminar"

    cv2.namedWindow(WINDOW)
    try:
        while True:
            frame = cam.read()
            faces = engine.detect_only(frame)
            view = cv2.resize(frame, (960, int(960 * frame.shape[0] / frame.shape[1])))
            scale = view.shape[1] / frame.shape[1]

            if len(faces) == 1:
                x1, y1, x2, y2 = (int(v * scale) for v in faces[0].bbox)
                cv2.rectangle(view, (x1, y1), (x2, y2), (0, 255, 0), 2)
                if active is not None:
                    active[3].append(anti_spoof.score(frame, faces[0]))
                    active[4].append(face_quality.lighting(frame, faces[0]).poor)
            elif len(faces) > 1:
                cv2.putText(view, "solo una cara en la camara", (10, 70),
                            cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 0, 255), 2)

            if active is not None:
                remaining = active[2] - time.monotonic()
                cv2.putText(view, f"GRABANDO {active[1]}  {remaining:0.1f}s  frames={len(active[3])}",
                            (10, 35), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 0, 255), 2)
                if remaining <= 0:
                    kind, label, _, scores, poor = active
                    if scores:
                        recordings.append({"kind": kind, "scores": [round(s, 4) for s in scores],
                                           "poor_light_ratio": float(np.mean(poor))})
                        last_status = f"guardado {label}: {len(scores)} frames, mediana {np.median(scores):.2f}"
                    else:
                        last_status = f"{label}: no se detecto ningun rostro, repite"
                    print(last_status)
                    active = None
            else:
                cv2.putText(view, last_status, (10, 35), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255, 255, 0), 2)
                counts = {}
                for r in recordings:
                    counts[r["kind"]] = counts.get(r["kind"], 0) + 1
                cv2.putText(view, " ".join(f"{k}={v}" for k, v in counts.items()) or "sin grabaciones",
                            (10, 65), cv2.FONT_HERSHEY_SIMPLEX, 0.55, (220, 220, 220), 1)

            cv2.imshow(WINDOW, view)
            key = cv2.waitKey(1) & 0xFF
            if key in (ord("q"), 27) or cv2.getWindowProperty(WINDOW, cv2.WND_PROP_VISIBLE) < 1:
                break
            if active is None and key in KINDS:
                kind, label = KINDS[key]
                active = (kind, label, time.monotonic() + GRABACION_S, [], [])
    finally:
        cam.release()
        cv2.destroyAllWindows()

    if not recordings:
        print("sin grabaciones")
        return
    rep = report(recordings)
    os.makedirs(os.path.join(BASE_DIR, "eval"), exist_ok=True)
    path = os.path.join(BASE_DIR, "eval", time.strftime("anti_spoof_%Y%m%d_%H%M%S.json"))
    with open(path, "w", encoding="utf-8") as fh:
        json.dump({"report": rep, "recordings": recordings}, fh, indent=2)
    print_report(rep, recordings)
    print(f"\nguardado en {path}")


if __name__ == "__main__":
    main()
