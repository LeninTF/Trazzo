from unittest.mock import MagicMock

import numpy as np
import pytest

from domain.errors import BiometricError, ErrorCode
from infrastructure import camera as camera_module
from infrastructure.camera import CameraCapture


def _fake_capture(opened, frame_ok=True):
    cap = MagicMock()
    cap.isOpened.return_value = opened
    frame = np.zeros((480, 640, 3), np.uint8)
    cap.read.return_value = (frame_ok, frame if frame_ok else None)
    return cap


@pytest.fixture(autouse=True)
def _camera_settings(override_setting):
    override_setting("camera_index", 1)
    override_setting("camera_warmup_frames", 0)
    override_setting("camera_read_timeout_s", 1.0)


def test_usa_otro_indice_si_el_configurado_no_existe(monkeypatch):
    created = {}

    def factory(index, _backend):
        cap = _fake_capture(opened=(index == 0))
        created[index] = cap
        return cap

    monkeypatch.setattr(camera_module.cv2, "VideoCapture", factory)

    frame = CameraCapture().read()

    assert frame.shape == (480, 640, 3)
    assert 1 in created and 0 in created
    created[1].release.assert_called_once()


def test_falla_si_no_hay_ninguna_camara(monkeypatch):
    monkeypatch.setattr(camera_module.cv2, "VideoCapture", lambda *_: _fake_capture(opened=False))

    with pytest.raises(BiometricError) as exc:
        CameraCapture().read()

    assert exc.value.code == ErrorCode.CAMERA_UNAVAILABLE


def test_descarta_camara_que_abre_pero_no_entrega_frames(monkeypatch):
    def factory(index, _backend):
        if index == 1:
            return _fake_capture(opened=True, frame_ok=False)
        return _fake_capture(opened=(index == 2))

    monkeypatch.setattr(camera_module.cv2, "VideoCapture", factory)

    assert CameraCapture().read().shape == (480, 640, 3)


class _SnappingCamera:
    """Cámara falsa: acepta hasta max_w x max_h y 'redondea' pedidos mayores a su máximo."""

    def __init__(self, max_w, max_h):
        self.max_w, self.max_h = max_w, max_h
        self.w, self.h = 640, 480

    def isOpened(self):
        return True

    def set(self, prop, value):
        import cv2
        if prop == cv2.CAP_PROP_FRAME_WIDTH:
            self.w = min(int(value), self.max_w)
        elif prop == cv2.CAP_PROP_FRAME_HEIGHT:
            self.h = min(int(value), self.max_h)
        return True

    def read(self):
        return True, np.zeros((self.h, self.w, 3), np.uint8)

    def grab(self):
        return True

    def release(self):
        pass


def test_elige_la_maxima_resolucion_soportada(monkeypatch):
    cam = _SnappingCamera(1920, 1080)
    monkeypatch.setattr(camera_module.cv2, "VideoCapture", lambda *_: cam)

    capture = CameraCapture()
    frame = capture.read()

    assert capture.resolution == (1920, 1080)
    assert frame.shape[:2] == (1080, 1920)


def test_respeta_resolucion_forzada_por_config(monkeypatch, override_setting):
    override_setting("camera_width", 1280)
    override_setting("camera_height", 720)
    cam = _SnappingCamera(3840, 2160)
    monkeypatch.setattr(camera_module.cv2, "VideoCapture", lambda *_: cam)

    capture = CameraCapture()
    capture.read()

    assert capture.resolution == (1280, 720)
