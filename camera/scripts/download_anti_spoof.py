"""Descarga los modelos ONNX de anti-spoofing.

Corre una sola vez despues de instalar el proyecto:
    .venv\\Scripts\\python scripts\\download_anti_spoof.py

Los modelos vienen del repo hairymax/Face-AntiSpoofing (MIT license),
convertidos a ONNX desde los pesos originales de Silent-Face-Anti-Spoofing.
"""
import os
import sys
import urllib.request

BASE = "https://github.com/hairymax/Face-AntiSpoofing/raw/main/saved_models"
FILES = [
    ("AntiSpoofing_print-replay_1.5_128.onnx", "modelo 3 clases: real / foto impresa / pantalla"),
    ("AntiSpoofing_bin_1.5_128.onnx",          "modelo binario: real / fake (ensemble)"),
]

TARGET_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "models", "anti_spoofing")


def _progress(count, block_size, total_size):
    if total_size <= 0:
        return
    done = int(50 * count * block_size / total_size)
    sys.stdout.write(f"\r  [{'#' * done}{'.' * (50 - done)}] {min(100, int(count * block_size * 100 / total_size))}%")
    sys.stdout.flush()


def main():
    os.makedirs(TARGET_DIR, exist_ok=True)
    for filename, desc in FILES:
        dst = os.path.join(TARGET_DIR, filename)
        if os.path.exists(dst) and os.path.getsize(dst) > 100_000:
            print(f"OK {filename} ya existe ({os.path.getsize(dst)//1024} KB)")
            continue
        url = f"{BASE}/{filename}"
        print(f"descargando {filename} ({desc})")
        print(f"  desde {url}")
        try:
            urllib.request.urlretrieve(url, dst, _progress)
            print(f"\n  -> {dst} ({os.path.getsize(dst)//1024} KB)")
        except Exception as e:
            print(f"\n  ERROR: {e}")
            if os.path.exists(dst):
                os.remove(dst)
            sys.exit(1)
    print("listo")


if __name__ == "__main__":
    main()
