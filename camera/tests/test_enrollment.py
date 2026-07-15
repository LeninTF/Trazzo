from unittest.mock import AsyncMock, MagicMock

import numpy as np
import pytest

from domain.models import CaptureResult, LivenessResult, LivenessSignals
from services.enrollment_service import EnrollmentService


def _cap_result():
    e = np.random.default_rng(0).standard_normal(512).astype(np.float32)
    e /= np.linalg.norm(e)
    return CaptureResult(
        embedding=e,
        liveness=LivenessResult(True, 0.9, LivenessSignals(0, 0, 0, 0)),
        frame_count=5,
        face_size=200,
    )


async def test_enroll_llama_replace_person(override_setting):
    override_setting("enrollment_samples", 2)

    capture = MagicMock()
    capture.capture = AsyncMock(side_effect=lambda *_a, **_kw: _cap_result())
    repo = MagicMock()

    service = EnrollmentService(capture, repo)
    result = await service.enroll("juan", "trazzo")

    assert result["personId"] == "juan"
    assert result["samples"] == 2
    assert repo.replace_person.call_count == 1
    face = repo.replace_person.call_args[0][0]
    assert face.person_id == "juan"
    assert face.tenant_id == "trazzo"
    assert np.linalg.norm(face.embedding) == pytest.approx(1.0, abs=1e-4)


async def test_enroll_captura_fallo_propaga(override_setting):
    from domain.errors import BiometricError, ErrorCode
    override_setting("enrollment_samples", 3)

    capture = MagicMock()
    capture.capture = AsyncMock(side_effect=BiometricError(ErrorCode.NO_FACE_DETECTED))
    repo = MagicMock()

    service = EnrollmentService(capture, repo)
    with pytest.raises(BiometricError):
        await service.enroll("juan", "trazzo")
    repo.replace_person.assert_not_called()


def test_delete_delega_al_repo():
    capture = MagicMock()
    repo = MagicMock()
    repo.delete_person = MagicMock(return_value=1)

    service = EnrollmentService(capture, repo)
    assert service.delete("juan", "trazzo") == 1
    repo.delete_person.assert_called_once_with("juan", "trazzo")
