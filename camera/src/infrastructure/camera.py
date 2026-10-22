import time
import logging
import threading

import cv2
import numpy as np

from config import SETTINGS
from domain.errors import BiometricError, ErrorCode

log = logging.getLogger(__name__)

# OpenCV crea por defecto un pool con TODOS los núcleos lógicos. Mientras la cámara decodifica
# MJPG, esos hilos compiten con los de ONNX Runtime y la inferencia se vuelve ~20x más lenta
# (medido: detección 3 ms -> 69 ms con la cámara abierta; 4 ms con este ajuste). La
# decodificación y los resize/warp de OpenCV no necesitan paralelismo propio. Es global al proceso.
cv2.setNumThreads(SETTINGS.opencv_threads)

_MAX_FALLBACK_INDEX = 4

# Resoluciones a probar, de mayor a menor. Se queda con la más alta que la cámara entregue
# de verdad (se valida con un frame real, no con lo que el driver dice aceptar).
_RESOLUTION_CANDIDATES = (
    (3840, 2160), (2560, 1440), (1920, 1080), (1600, 1200),
    (1280, 960), (1280, 720), (1024, 768), (800, 600), (640, 480),
)
_TARGET_FPS = 30

_BACKENDS = {
    "dshow": cv2.CAP_DSHOW,
    "msmf": cv2.CAP_MSMF,
    "any": cv2.CAP_ANY,
}


class CameraCapture:
    def __init__(self) -> None:
        self._cap: cv2.VideoCapture | None = None
        self._lock = threading.Lock()
        self._closed = False
        self.resolution: tuple[int, int] = (0, 0)

    @staticmethod
    def _configure_max_resolution(cap) -> tuple[int, int]:
        """Pone la cámara en la mayor resolución que entregue realmente.

        Más píxeles = recorte facial de mejor calidad para el reconocimiento y el anti-spoof,
        sobre todo con la persona algo alejada. OpenCV/DirectShow por defecto usa 640x480.
        """
        if SETTINGS.camera_width > 0 and SETTINGS.camera_height > 0:
            candidates = ((SETTINGS.camera_width, SETTINGS.camera_height),)
        else:
            candidates = _RESOLUTION_CANDIDATES

        # MJPG: sin compresión en el USB, las resoluciones altas caen a pocos fps (o fallan).
        cap.set(cv2.CAP_PROP_FOURCC, cv2.VideoWriter_fourcc(*"MJPG"))

        best: tuple[int, int] | None = None
        for req_w, req_h in candidates:
            cap.set(cv2.CAP_PROP_FRAME_WIDTH, req_w)
            cap.set(cv2.CAP_PROP_FRAME_HEIGHT, req_h)
            ok, frame = cap.read()
            if not ok or frame is None or frame.size == 0:
                continue
            got_h, got_w = frame.shape[:2]
            if best is None or got_w * got_h > best[0] * best[1]:
                best = (got_w, got_h)
            if (got_w, got_h) == (req_w, req_h):
                break  # aceptó exactamente la mayor resolución de la lista que soporta

        if best is None:
            # la cámara abrió pero no aceptó ninguna configuración: dejarla en su modo por defecto
            ok, frame = cap.read()
            return (frame.shape[1], frame.shape[0]) if ok and frame is not None else (0, 0)

        # dejar la cámara fija en la mejor resolución encontrada
        cap.set(cv2.CAP_PROP_FRAME_WIDTH, best[0])
        cap.set(cv2.CAP_PROP_FRAME_HEIGHT, best[1])
        cap.set(cv2.CAP_PROP_FPS, _TARGET_FPS)
        return best

    @staticmethod
    def _try_open(index: int, backend: int):
        """Abre un índice y confirma que entrega un frame real. Devuelve el capture o None."""
        cap = cv2.VideoCapture(index, backend)
        if not cap.isOpened():
            cap.release()
            return None
        ok, frame = cap.read()
        if not ok or frame is None or frame.size == 0:
            cap.release()
            return None
        return cap

    def _open_locked(self) -> None:
        backend = _BACKENDS.get(SETTINGS.camera_backend.lower(), cv2.CAP_DSHOW)

        # Primero el índice configurado; si no existe, los primeros índices del sistema.
        # CAMERA_INDEX=1 por defecto no existe en la mayoría de PCs con una sola webcam.
        candidates = [SETTINGS.camera_index] + [i for i in range(_MAX_FALLBACK_INDEX) if i != SETTINGS.camera_index]
        for index in candidates:
            cap = self._try_open(index, backend)
            if cap is None:
                continue
            if index != SETTINGS.camera_index:
                log.warning("camara indice %d no disponible; usando indice %d",
                            SETTINGS.camera_index, index)

            width, height = self._configure_max_resolution(cap)

            cap.set(cv2.CAP_PROP_BUFFERSIZE, 1)
            for _ in range(SETTINGS.camera_warmup_frames):
                cap.grab()

            self._cap = cap
            self.resolution = (width, height)
            log.info("camara %d abierta (backend=%s, resolucion=%dx%d)",
                     index, SETTINGS.camera_backend, width, height)
            return

        raise BiometricError(
            ErrorCode.CAMERA_UNAVAILABLE,
            f"no hay camara disponible (probados indices {candidates})",
        )

    def is_open(self) -> bool:
        with self._lock:
            return self._cap is not None and self._cap.isOpened()

    def read(self) -> np.ndarray:
        with self._lock:
            if self._closed:
                raise BiometricError(ErrorCode.CAMERA_UNAVAILABLE, "camara cerrada")
            if self._cap is None or not self._cap.isOpened():
                self._open_locked()

            cap = self._cap
            if cap is None:
                raise BiometricError(ErrorCode.CAMERA_UNAVAILABLE)

            # El plazo se calcula DESPUÉS de abrir: abrir + warmup puede tardar más que el
            # timeout de lectura y dejaba la primera lectura sin tiempo.
            deadline = time.monotonic() + SETTINGS.camera_read_timeout_s
            while time.monotonic() < deadline:
                cap.grab()  # descarta el buffer viejo
                ok, frame = cap.read()
                if ok and frame is not None and frame.size > 0:
                    return frame

            raise BiometricError(ErrorCode.CAMERA_TIMEOUT, "camara no respondio a tiempo")

    def release(self) -> None:
        with self._lock:
            self._closed = True
            if self._cap is not None:
                try:
                    self._cap.release()
                finally:
                    self._cap = None
                    log.info("camara liberada")
