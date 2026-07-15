from unittest.mock import AsyncMock, MagicMock

import numpy as np
import pytest

from domain.errors import BiometricError, ErrorCode
from domain.models import CaptureResult, LivenessResult, LivenessSignals
from services.recognition_service import RecognitionService, _cosine


def _capture_result(emb):
    return CaptureResult(
        embedding=emb,
        liveness=LivenessResult(True, 0.9, LivenessSignals(0, 0, 0, 0)),
        frame_count=5,
        face_size=200,
    )


def _make_service(capture_emb, stored=None, gallery=None):
    capture = MagicMock()
    result = _capture_result(capture_emb)
    capture.capture = AsyncMock(return_value=result)
    capture.capture_quick = AsyncMock(return_value=result)
    capture.capture_full = AsyncMock(return_value=result)
    capture.capture_passive = AsyncMock(return_value=result)

    repo = MagicMock()
    repo.load_person = MagicMock(return_value=stored or [])
    repo.iter_gallery = MagicMock(return_value=iter(gallery or []))
    return RecognitionService(capture, repo)


def _norm(v):
    return v / np.linalg.norm(v)


class TestCosine:
    def test_cosine_de_vector_consigo_es_1(self):
        v = _norm(np.random.default_rng(0).standard_normal(512).astype(np.float32))
        assert _cosine(v, v) == pytest.approx(1.0, abs=1e-5)

    def test_cosine_de_vectores_ortogonales_es_0(self):
        a = np.array([1, 0, 0], np.float32)
        b = np.array([0, 1, 0], np.float32)
        assert _cosine(a, b) == 0.0


class TestVerify:
    async def test_persona_no_enrolada(self):
        service = _make_service(np.zeros(512, np.float32), stored=[])
        with pytest.raises(BiometricError) as exc:
            await service.verify("juan", "trazzo")
        assert exc.value.code == ErrorCode.PERSON_NOT_ENROLLED

    async def test_match_exitoso(self):
        emb = _norm(np.random.default_rng(0).standard_normal(512).astype(np.float32))
        service = _make_service(emb, stored=[emb])
        match, meta = await service.verify("juan", "trazzo")
        assert match.person_id == "juan"
        assert match.score > 0.99
        assert meta["liveness"] == 0.9

    async def test_no_match_bajo_umbral(self):
        rng = np.random.default_rng(0)
        e1 = _norm(rng.standard_normal(512).astype(np.float32))
        e2 = _norm(rng.standard_normal(512).astype(np.float32))
        service = _make_service(e1, stored=[e2])
        with pytest.raises(BiometricError) as exc:
            await service.verify("juan", "trazzo")
        assert exc.value.code == ErrorCode.NO_MATCH


class TestIdentify:
    async def test_galeria_vacia(self):
        service = _make_service(np.zeros(512, np.float32), gallery=[])
        with pytest.raises(BiometricError) as exc:
            await service.identify("trazzo")
        assert exc.value.code == ErrorCode.NO_ENROLLED_FACES

    async def test_identifica_al_mejor(self):
        rng = np.random.default_rng(0)
        target = _norm(rng.standard_normal(512).astype(np.float32))
        otros = [_norm(rng.standard_normal(512).astype(np.float32)) for _ in range(3)]
        gallery = [("pedro", otros[0]), ("juan", target), ("maria", otros[1])]
        service = _make_service(target, gallery=gallery)
        match, _ = await service.identify("trazzo")
        assert match.person_id == "juan"

    async def test_no_match_si_ninguno_pasa_umbral(self):
        rng = np.random.default_rng(0)
        probe = _norm(rng.standard_normal(512).astype(np.float32))
        # 3 embeddings aleatorios, ninguno cercano al probe
        gallery = [(f"p{i}", _norm(rng.standard_normal(512).astype(np.float32))) for i in range(3)]
        service = _make_service(probe, gallery=gallery)
        with pytest.raises(BiometricError) as exc:
            await service.identify("trazzo")
        assert exc.value.code == ErrorCode.NO_MATCH
