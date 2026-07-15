"""Anti-spoofing con CNN entrenada (MiniFASNet ensemble).

Ensemble de:
  1. print-replay 3-clases (real / foto impresa / pantalla)
  2. binario 2-clases (real / fake)

Ambos con crop escalado a 1.5x, letterbox a 128x128, sin conversion de color.
Convencion del repo hairymax/Face-AntiSpoofing: REAL = clase 0.
"""
import logging
import os
import threading
from typing import Final

import cv2
import numpy as np
import onnxruntime as ort

from config import SETTINGS

log = logging.getLogger(__name__)

_MODELS: Final = (
    "AntiSpoofing_print-replay_1.5_128.onnx",
    "AntiSpoofing_bin_1.5_128.onnx",
)
_BBOX_SCALE: Final[float] = 1.5
_INPUT_SIZE: Final[int] = 128
_REAL_CLASS_IDX: Final[int] = 0


def _softmax(x: np.ndarray) -> np.ndarray:
    e = np.exp(x - x.max())
    return e / e.sum()


def _letterbox(img: np.ndarray, size: int) -> np.ndarray:
    h, w = img.shape[:2]
    if h == 0 or w == 0:
        return np.zeros((size, size, 3), np.uint8)
    scale = size / max(h, w)
    nh, nw = max(1, int(h * scale)), max(1, int(w * scale))
    resized = cv2.resize(img, (nw, nh))
    canvas = np.zeros((size, size, 3), np.uint8)
    y = (size - nh) // 2
    x = (size - nw) // 2
    canvas[y:y + nh, x:x + nw] = resized
    return canvas


def _safe_model_path(directory: str, filename: str) -> str:
    """Previene path traversal: fuerza al archivo a estar dentro del directory."""
    full = os.path.abspath(os.path.join(directory, filename))
    root = os.path.abspath(directory)
    if not full.startswith(root + os.sep):
        raise ValueError(f"path traversal detectado: {full}")
    return full


class AntiSpoofingService:
    _instance: "AntiSpoofingService | None" = None
    _lock = threading.Lock()

    def __init__(self) -> None:
        self._sessions: list[tuple[ort.InferenceSession, str]] = []
        directory = os.path.join(SETTINGS.models_dir, "anti_spoofing")

        for name in _MODELS:
            try:
                path = _safe_model_path(directory, name)
            except ValueError as e:
                log.error("modelo rechazado por seguridad: %s", e)
                continue

            if not os.path.isfile(path):
                log.warning("modelo anti-spoof no encontrado: %s", path)
                continue

            so = ort.SessionOptions()
            so.intra_op_num_threads = 1
            so.graph_optimization_level = ort.GraphOptimizationLevel.ORT_ENABLE_BASIC
            try:
                sess = ort.InferenceSession(path, sess_options=so, providers=["CPUExecutionProvider"])
            except (ort.capi.onnxruntime_pybind11_state.Fail, RuntimeError) as e:
                log.error("no se pudo cargar %s: %s", name, e)
                continue

            self._sessions.append((sess, sess.get_inputs()[0].name))
            log.info("anti-spoof modelo cargado: %s", name)

        self.enabled = len(self._sessions) > 0
        if not self.enabled:
            log.warning("AntiSpoofingService DESHABILITADO — corre scripts/download_anti_spoof.py")

    @classmethod
    def instance(cls) -> "AntiSpoofingService":
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    cls._instance = cls()
        return cls._instance

    def _crop(self, frame: np.ndarray, face) -> np.ndarray:
        h, w = frame.shape[:2]
        x1, y1, x2, y2 = face.bbox
        cx = (x1 + x2) / 2
        cy = (y1 + y2) / 2
        half = max(x2 - x1, y2 - y1) * _BBOX_SCALE / 2
        nx1 = max(0, int(cx - half))
        ny1 = max(0, int(cy - half))
        nx2 = min(w, int(cx + half))
        ny2 = min(h, int(cy + half))
        return frame[ny1:ny2, nx1:nx2]

    def _preprocess(self, crop: np.ndarray) -> np.ndarray:
        img = _letterbox(crop, _INPUT_SIZE)
        img = img.astype(np.float32) / 255.0
        img = np.transpose(img, (2, 0, 1))
        return np.expand_dims(img, axis=0)

    def score(self, frame: np.ndarray, face) -> float:
        """Probabilidad [0..1] de que la imagen sea real.
        Ensemble por MIN: ambos modelos deben coincidir. Si uno dice "fake",
        el score cae al minimo — es lo correcto para seguridad (falsos positivos
        pesan mas que falsos negativos).
        Fail-open si no hay modelos (con warning ya loggeado).
        """
        if not self.enabled:
            return 1.0

        crop = self._crop(frame, face)
        if crop.size == 0:
            return 0.0

        inp = self._preprocess(crop)
        real_probs: list[float] = []
        for sess, input_name in self._sessions:
            out = sess.run(None, {input_name: inp})[0][0]
            probs = _softmax(out)
            real_probs.append(float(probs[_REAL_CLASS_IDX]))

        return float(np.min(real_probs))
