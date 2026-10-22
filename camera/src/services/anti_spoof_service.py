"""Anti-spoofing con CNN entrenada (MiniFASNet ensemble).

Ensemble de:
  1. print-replay 3-clases (real / foto impresa / pantalla)
  2. binario 2-clases (real / fake)

Ambos con crop cuadrado escalado a 1.5x, 128x128, en RGB (entrenados así).
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
            so.inter_op_num_threads = 1
            so.add_session_config_entry("session.intra_op.allow_spinning", "0")
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
            if SETTINGS.anti_spoof_required:
                log.critical(
                    "ANTI-SPOOF CNN NO CARGADO y ANTI_SPOOF_REQUIRED=1 -> toda captura sera "
                    "rechazada. Corre scripts/download_anti_spoof.py.")
            else:
                log.warning(
                    "AntiSpoofingService DESHABILITADO (fail-open) — el CNN no protege contra "
                    "fotos/pantallas. Corre scripts/download_anti_spoof.py o setea "
                    "ANTI_SPOOF_REQUIRED=1 para bloquear.")

    @classmethod
    def instance(cls) -> "AntiSpoofingService":
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    cls._instance = cls()
        return cls._instance

    def _crop(self, frame: np.ndarray, face) -> np.ndarray:
        """Recorte CUADRADO de 1.5x el rostro, relleno en negro si sale del frame.

        Replica `increased_crop` del repo de referencia: los modelos se entrenaron con el
        rostro centrado en un cuadrado; recortar pegado al borde lo descentra y baja el score.
        """
        h, w = frame.shape[:2]
        x1, y1, x2, y2 = (float(v) for v in face.bbox)
        side = int(max(x2 - x1, y2 - y1) * _BBOX_SCALE)
        if side <= 0:
            return frame[0:0, 0:0]
        left = int((x1 + x2) / 2 - side / 2)
        top = int((y1 + y2) / 2 - side / 2)
        cx1, cy1 = max(0, left), max(0, top)
        cx2, cy2 = min(w, left + side), min(h, top + side)
        if cx2 <= cx1 or cy2 <= cy1:
            return frame[0:0, 0:0]
        return cv2.copyMakeBorder(
            frame[cy1:cy2, cx1:cx2],
            cy1 - top, (top + side) - cy2, cx1 - left, (left + side) - cx2,
            cv2.BORDER_CONSTANT, value=(0, 0, 0),
        )

    def _preprocess(self, crop: np.ndarray) -> np.ndarray:
        # OpenCV entrega BGR; los modelos se entrenaron con RGB. Sin esta conversión un rostro
        # real se clasificaba como "foto impresa" (medido: real 0.16 en BGR vs 0.79 en RGB).
        img = cv2.cvtColor(_letterbox(crop, _INPUT_SIZE), cv2.COLOR_BGR2RGB)
        img = img.astype(np.float32) / 255.0
        img = np.transpose(img, (2, 0, 1))
        return np.expand_dims(img, axis=0)

    def score(self, frame: np.ndarray, face) -> float:
        """Probabilidad [0..1] de que la imagen (frame BGR) sea real.

        Ensemble por PROMEDIO (soft voting) de los dos modelos. El mínimo quedaba dominado por
        el modelo binario, más débil: con una persona real su mediana era 0.69 y rechazaba
        frames legítimos. Un solo frame no decide: las capturas descartan frames sueltos y
        solo abortan tras una racha de rechazos, y el liveness activo/pasivo es otra barrera.
        Sin modelos: fail-open (1.0) por defecto, o fail-closed (0.0) si
        ANTI_SPOOF_REQUIRED=1 — así ninguna captura pasa sin protección real.
        """
        if not self.enabled:
            return 0.0 if SETTINGS.anti_spoof_required else 1.0

        crop = self._crop(frame, face)
        if crop.size == 0:
            return 0.0

        inp = self._preprocess(crop)
        real_probs: list[float] = []
        for sess, input_name in self._sessions:
            out = sess.run(None, {input_name: inp})[0][0]
            probs = _softmax(out)
            real_probs.append(float(probs[_REAL_CLASS_IDX]))

        return float(np.mean(real_probs))
