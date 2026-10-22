"""Prepara el pack de reconocimiento facial `trazzo_r50`.

Corre una sola vez por equipo (idempotente):
    .venv\\Scripts\\python scripts\\download_models.py

`trazzo_r50` combina:
  - det_500m.onnx   detector SCRFD liviano (del pack buffalo_sc, ya incluido en el repo)
  - w600k_r50.onnx  reconocedor ArcFace ResNet-50 entrenado en WebFace600K (del pack buffalo_l)

Motivo: para una cámara de marcación (una persona cerca y de frente) el detector liviano rinde
igual que el pesado, mientras que el reconocedor ResNet-50 distingue personas mucho mejor que el
MobileFaceNet de buffalo_sc. Del zip de buffalo_l (~290 MB) solo se extrae el reconocedor; los
modelos de landmarks 3D y edad/género no se usan.

Además descarga los modelos anti-spoofing si faltan (scripts/download_anti_spoof.py).
"""
import os
import shutil
import sys
import urllib.request
import zipfile

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PACKS_DIR = os.path.join(BASE_DIR, "models", "models")
PACK_NAME = "trazzo_r50"
PACK_DIR = os.path.join(PACKS_DIR, PACK_NAME)

DETECTOR_SRC = os.path.join(PACKS_DIR, "buffalo_sc", "det_500m.onnx")
BUFFALO_L_ZIP = os.path.join(PACKS_DIR, "buffalo_l.zip")
BUFFALO_L_URL = "https://github.com/deepinsight/insightface/releases/download/v0.7/buffalo_l.zip"
RECOGNIZER = "w600k_r50.onnx"

# tamaños esperados: detectan descargas truncadas o archivos de error HTML
_MIN_BYTES = {"det_500m.onnx": 2_000_000, RECOGNIZER: 170_000_000}


def _ok(path: str) -> bool:
    return os.path.isfile(path) and os.path.getsize(path) >= _MIN_BYTES[os.path.basename(path)]


def _progress(count, block_size, total_size):
    if total_size > 0:
        pct = min(100, int(count * block_size * 100 / total_size))
        sys.stdout.write(f"\r  descargando buffalo_l.zip... {pct}%")
        sys.stdout.flush()


def _ensure_recognizer() -> None:
    target = os.path.join(PACK_DIR, RECOGNIZER)
    if _ok(target):
        print(f"OK {RECOGNIZER} ya existe")
        return

    if not (os.path.isfile(BUFFALO_L_ZIP) and zipfile.is_zipfile(BUFFALO_L_ZIP)):
        print(f"descargando {BUFFALO_L_URL}")
        tmp = BUFFALO_L_ZIP + ".part"
        urllib.request.urlretrieve(BUFFALO_L_URL, tmp, _progress)
        print()
        os.replace(tmp, BUFFALO_L_ZIP)

    with zipfile.ZipFile(BUFFALO_L_ZIP) as zf:
        member = next((n for n in zf.namelist() if n.endswith(RECOGNIZER)), None)
        if member is None:
            raise SystemExit(f"ERROR: {RECOGNIZER} no está dentro de {BUFFALO_L_ZIP}")
        tmp = target + ".part"
        with zf.open(member) as src, open(tmp, "wb") as dst:
            shutil.copyfileobj(src, dst, length=1024 * 1024)
        os.replace(tmp, target)
    if not _ok(target):
        raise SystemExit(f"ERROR: {RECOGNIZER} quedó incompleto")
    print(f"OK {RECOGNIZER} extraído ({os.path.getsize(target) // (1024 * 1024)} MB)")


def _ensure_detector() -> None:
    target = os.path.join(PACK_DIR, "det_500m.onnx")
    if _ok(target):
        print("OK det_500m.onnx ya existe")
        return
    if not _ok(DETECTOR_SRC):
        raise SystemExit(f"ERROR: falta {DETECTOR_SRC} (viene con el repositorio)")
    shutil.copyfile(DETECTOR_SRC, target)
    print("OK det_500m.onnx copiado desde buffalo_sc")


def main() -> None:
    os.makedirs(PACK_DIR, exist_ok=True)
    _ensure_detector()
    _ensure_recognizer()
    print(f"pack '{PACK_NAME}' listo en {PACK_DIR}")

    anti_spoof = os.path.join(BASE_DIR, "scripts", "download_anti_spoof.py")
    if os.path.isfile(anti_spoof):
        print("verificando modelos anti-spoofing...")
        import runpy
        runpy.run_path(anti_spoof, run_name="__main__")


if __name__ == "__main__":
    main()
