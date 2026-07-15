from unittest.mock import MagicMock

import numpy as np
import pytest

from domain.errors import BiometricError, ErrorCode
from domain.models import DetectedFace
from services.capture_service import CaptureService


def _face(embedding=None, bbox=(100, 100, 300, 300), kps=None):
    if embedding is None:
        embedding = np.random.default_rng(0).standard_normal(512).astype(np.float32)
        embedding /= np.linalg.norm(embedding)
    if kps is None:
        kps = np.array([[150, 150], [250, 150], [200, 200], [170, 250], [230, 250]], np.float32)
    return DetectedFace(
        bbox=np.array(bbox, np.float32),
        keypoints=kps,
        embedding=embedding,
        detection_score=0.99,
    )


def _kps_frontal():
    # nariz centrada entre los ojos (yaw ~= 0)
    return np.array([[150, 150], [250, 150], [200, 200], [170, 250], [230, 250]], np.float32)


def _kps_turned_left():
    # usuario mira su izquierda -> nariz a la derecha del centro
    return np.array([[150, 150], [250, 150], [235, 200], [170, 250], [230, 250]], np.float32)


def _rich_frame():
    return np.random.default_rng(0).integers(0, 255, (480, 640, 3), np.uint8)


@pytest.fixture
def cam_that_returns_frames():
    m = MagicMock()
    m.read = MagicMock(side_effect=lambda: _rich_frame())
    return m


@pytest.fixture(autouse=True)
def _fast_capture(override_setting, monkeypatch):
    override_setting("capture_max_seconds", 1.0)
    override_setting("capture_min_valid_frames", 3)
    # bajar timeouts del challenge para que los tests corran rapido
    from services import challenge_service
    monkeypatch.setattr(challenge_service, "_STAGE_TIMEOUT_S", 1.0)
    monkeypatch.setattr(challenge_service, "_STABLE_FRAMES", 1)


def _engine_sequence(seq):
    """MagicMock cuyo detect() devuelve elementos de seq (uno por llamada, repite el ultimo)."""
    engine = MagicMock()
    calls = {"i": 0}

    def _detect(_frame):
        if calls["i"] < len(seq):
            r = seq[calls["i"]]
            calls["i"] += 1
        else:
            r = seq[-1]
        return r

    engine.detect = MagicMock(side_effect=_detect)
    return engine


async def test_challenge_exitoso(cam_that_returns_frames, mock_liveness, mock_anti_spoof, monkeypatch):
    # forzar direccion determinista
    from services import challenge_service
    monkeypatch.setattr(challenge_service.random, "SystemRandom",
                        lambda: MagicMock(choice=lambda _l: challenge_service.Direction.LEFT))

    e = np.random.default_rng(0).standard_normal(512).astype(np.float32)
    e /= np.linalg.norm(e)

    # secuencia: frontal, frontal, girado, girado, frontal, frontal
    engine = _engine_sequence([
        [_face(embedding=e, kps=_kps_frontal())],
        [_face(embedding=e, kps=_kps_turned_left())],
        [_face(embedding=e, kps=_kps_frontal())],
    ])

    service = CaptureService(cam_that_returns_frames, engine, mock_liveness, mock_anti_spoof)
    result = await service.capture()
    assert result.embedding.shape == (512,)
    assert np.linalg.norm(result.embedding) == pytest.approx(1.0, abs=1e-5)


async def test_challenge_falla_si_no_gira(cam_that_returns_frames, mock_liveness, mock_anti_spoof):
    # engine devuelve siempre frontal — el usuario nunca gira
    engine = _engine_sequence([[_face(kps=_kps_frontal())]])
    service = CaptureService(cam_that_returns_frames, engine, mock_liveness, mock_anti_spoof)
    with pytest.raises(BiometricError) as exc:
        await service.capture()
    assert exc.value.code == ErrorCode.SPOOF_DETECTED


async def test_anti_spoof_rechaza_durante_frontal(cam_that_returns_frames, mock_liveness):
    anti_spoof = MagicMock()
    anti_spoof.score.return_value = 0.1

    engine = _engine_sequence([[_face(kps=_kps_frontal())]])
    service = CaptureService(cam_that_returns_frames, engine, mock_liveness, anti_spoof)
    with pytest.raises(BiometricError) as exc:
        await service.capture()
    assert exc.value.code == ErrorCode.SPOOF_DETECTED
