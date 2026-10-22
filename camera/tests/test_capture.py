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


async def test_captura_pasiva_rechaza_ataque_intermitente(cam_that_returns_frames, mock_liveness):
    """Un ataque que alterna frames 'reales' y 'falsos' nunca completa la racha de rechazos;
    la fusión temporal (mediana) debe rechazarlo igual."""
    anti_spoof = MagicMock()
    anti_spoof.score.side_effect = [0.95, 0.1, 0.1] * 20
    engine = _engine_sequence([[_face()]])
    service = CaptureService(cam_that_returns_frames, engine, mock_liveness, anti_spoof)

    with pytest.raises(BiometricError) as exc:
        await service.capture_passive()
    assert exc.value.code == ErrorCode.SPOOF_DETECTED
    assert "fusionado" in str(exc.value)


async def test_captura_pasiva_rechaza_si_cambia_la_persona(cam_that_returns_frames, mock_liveness, mock_anti_spoof):
    """Si otra persona entra durante la ventana de captura, no se promedian dos rostros."""
    rng = np.random.default_rng(5)
    a = rng.standard_normal(512).astype(np.float32)
    a /= np.linalg.norm(a)
    b = rng.standard_normal(512).astype(np.float32)
    b /= np.linalg.norm(b)

    # mitad de los frames es una persona, la otra mitad es otra
    engine = _engine_sequence(
        [[_face(embedding=a)]] * 4 + [[_face(embedding=b)]] * 4
    )
    service = CaptureService(cam_that_returns_frames, engine, mock_liveness, mock_anti_spoof)

    with pytest.raises(BiometricError) as exc:
        await service.capture_passive()
    assert exc.value.code == ErrorCode.IDENTITY_INCONSISTENT


async def test_capturas_concurrentes_se_serializan(mock_liveness, mock_anti_spoof, monkeypatch):
    """La cámara es un recurso único: dos capturas no deben correr a la vez."""
    import asyncio
    service = CaptureService(MagicMock(), MagicMock(), mock_liveness, mock_anti_spoof)

    activas = {"n": 0, "max": 0}

    async def fake_run_passive(_self):
        activas["n"] += 1
        activas["max"] = max(activas["max"], activas["n"])
        await asyncio.sleep(0.05)
        activas["n"] -= 1
        e = np.ones(512, np.float32) / np.sqrt(512)
        return e, {"mode": "passive", "frames": 8, "motion": 1.0, "cnn_min": 0.9}

    from services import challenge_service
    monkeypatch.setattr(challenge_service.ActiveLivenessSession, "run_passive", fake_run_passive)

    await asyncio.gather(service.capture_passive(), service.capture_passive(), service.capture_passive())

    assert activas["max"] == 1
