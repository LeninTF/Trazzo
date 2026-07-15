import os
import sys
from unittest.mock import MagicMock

import numpy as np
import pytest


ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "src")
if SRC not in sys.path:
    sys.path.insert(0, SRC)


_SETTINGS_FIELDS_TO_SNAPSHOT = (
    "db_path",
    "enrollment_samples",
    "max_gallery_per_tenant",
    "capture_max_seconds",
    "capture_min_valid_frames",
    "min_face_size",
    "liveness_min_score",
    "anti_spoof_min_real",
    "recognition_threshold",
)


@pytest.fixture(autouse=True)
def _isolate_env(tmp_path):
    """Aisla DB y master.key en tmp_path, restaura SETTINGS y singletons."""
    from config import SETTINGS
    from infrastructure.crypto import CryptoService

    snapshot = {f: getattr(SETTINGS, f) for f in _SETTINGS_FIELDS_TO_SNAPSHOT}
    object.__setattr__(SETTINGS, "db_path", str(tmp_path / "faces.db"))
    CryptoService._instance = None

    yield

    for k, v in snapshot.items():
        object.__setattr__(SETTINGS, k, v)
    CryptoService._instance = None

    # relajar permisos para que pytest pueda limpiar tmp_path
    for root, _dirs, files in os.walk(tmp_path):
        for name in files:
            try:
                os.chmod(os.path.join(root, name), 0o666)
            except OSError:
                pass


@pytest.fixture
def override_setting():
    """Modifica un campo del SETTINGS frozen; el autouse restaura al final."""
    from config import SETTINGS

    def _set(name, value):
        object.__setattr__(SETTINGS, name, value)

    return _set


@pytest.fixture
def repo():
    from infrastructure.db import FaceRepository
    r = FaceRepository()
    yield r
    r.close()


@pytest.fixture
def crypto():
    from infrastructure.crypto import CryptoService
    return CryptoService.instance()


@pytest.fixture
def emb():
    v = np.random.default_rng(42).standard_normal(512).astype(np.float32)
    v /= np.linalg.norm(v)
    return v


@pytest.fixture
def fake_face():
    from domain.models import DetectedFace
    return DetectedFace(
        bbox=np.array([100, 100, 300, 300], np.float32),
        keypoints=np.array([[150, 150], [250, 150], [200, 200], [170, 250], [230, 250]], np.float32),
        embedding=np.random.default_rng(0).standard_normal(512).astype(np.float32),
        detection_score=0.99,
    )


@pytest.fixture
def rng_frame():
    return np.random.default_rng(0).integers(0, 255, (480, 640, 3), np.uint8)


@pytest.fixture
def mock_camera():
    m = MagicMock()
    m.is_open.return_value = True
    return m


@pytest.fixture
def mock_engine(fake_face):
    m = MagicMock()
    m.detect.return_value = [fake_face]
    return m


@pytest.fixture
def mock_liveness():
    from domain.models import LivenessResult, LivenessSignals
    m = MagicMock()
    m.evaluate.return_value = LivenessResult(
        is_live=True, score=0.9,
        signals=LivenessSignals(150.0, 0.5, 0.5, 0.95),
    )
    return m


@pytest.fixture
def mock_anti_spoof():
    m = MagicMock()
    m.score.return_value = 0.95
    return m
