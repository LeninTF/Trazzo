import time
import logging
import threading

import cv2
import numpy as np

from config import SETTINGS
from domain.errors import BiometricError, ErrorCode

log = logging.getLogger(__name__)

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

    def _open_locked(self) -> None:
        backend = _BACKENDS.get(SETTINGS.camera_backend.lower(), cv2.CAP_DSHOW)
        cap = cv2.VideoCapture(SETTINGS.camera_index, backend)

        deadline = time.monotonic() + SETTINGS.camera_open_timeout_s
        while not cap.isOpened() and time.monotonic() < deadline:
            time.sleep(0.05)

        if not cap.isOpened():
            cap.release()
            raise BiometricError(
                ErrorCode.CAMERA_UNAVAILABLE,
                f"no puedo abrir camara indice {SETTINGS.camera_index}",
            )

        cap.set(cv2.CAP_PROP_BUFFERSIZE, 1)
        for _ in range(SETTINGS.camera_warmup_frames):
            cap.grab()

        self._cap = cap
        log.info("camara %d abierta (backend=%s)", SETTINGS.camera_index, SETTINGS.camera_backend)

    def is_open(self) -> bool:
        with self._lock:
            return self._cap is not None and self._cap.isOpened()

    def read(self) -> np.ndarray:
        deadline = time.monotonic() + SETTINGS.camera_read_timeout_s
        with self._lock:
            if self._closed:
                raise BiometricError(ErrorCode.CAMERA_UNAVAILABLE, "camara cerrada")
            if self._cap is None or not self._cap.isOpened():
                self._open_locked()

            cap = self._cap
            if cap is None:
                raise BiometricError(ErrorCode.CAMERA_UNAVAILABLE)

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
