import numpy as np
import pytest

from domain.models import DetectedFace
from services.liveness_service import (
    LivenessService, _lap_var, _crop, _fft_high_freq_ratio, _skin_ratio, _consistency,
)


def _face(embedding=None, bbox=(100, 100, 300, 300)):
    if embedding is None:
        embedding = np.random.default_rng(0).standard_normal(512).astype(np.float32)
        embedding /= np.linalg.norm(embedding)
    return DetectedFace(
        bbox=np.array(bbox, np.float32),
        keypoints=np.array([[150, 150], [250, 150], [200, 200], [170, 250], [230, 250]], np.float32),
        embedding=embedding,
        detection_score=0.99,
    )


def _rich_frame():
    """Frame con textura variada (mucho detalle)."""
    return np.random.default_rng(0).integers(0, 255, (480, 640, 3), np.uint8)


def _flat_frame():
    """Frame casi plano (baja textura, tipico de foto impresa borrosa)."""
    return np.full((480, 640, 3), 128, np.uint8)


class TestLivenessSignals:
    def test_laplacian_alto_en_frame_con_ruido(self):
        assert _lap_var(_rich_frame()) > 100

    def test_laplacian_bajo_en_frame_plano(self):
        assert _lap_var(_flat_frame()) < 5

    def test_fft_ratio_en_rango_0_1(self):
        r = _fft_high_freq_ratio(_rich_frame())
        assert 0.0 <= r <= 1.0

    def test_skin_ratio_en_rango_0_1(self):
        r = _skin_ratio(_rich_frame())
        assert 0.0 <= r <= 1.0

    def test_consistency_1_si_una_sola_cara(self):
        assert _consistency([_face()]) == 1.0

    def test_consistency_alto_para_misma_persona(self):
        e = np.random.default_rng(1).standard_normal(512).astype(np.float32)
        e /= np.linalg.norm(e)
        faces = [_face(embedding=e), _face(embedding=e), _face(embedding=e)]
        assert _consistency(faces) > 0.99

    def test_consistency_bajo_para_diferentes_personas(self):
        rng = np.random.default_rng(0)
        faces = []
        for _ in range(3):
            e = rng.standard_normal(512).astype(np.float32)
            e /= np.linalg.norm(e)
            faces.append(_face(embedding=e))
        assert _consistency(faces) < 0.6


class TestLivenessService:
    def test_evaluate_sin_frames_devuelve_no_live(self):
        r = LivenessService().evaluate([], [])
        assert r.is_live is False

    def test_evaluate_con_frames_iguales_rechaza_por_inconsistencia(self):
        # embeddings totalmente distintos => consistency < 0.75 => rechaza
        service = LivenessService()
        rng = np.random.default_rng(0)
        frames, faces = [], []
        for _ in range(4):
            frames.append(_rich_frame())
            e = rng.standard_normal(512).astype(np.float32)
            e /= np.linalg.norm(e)
            faces.append(_face(embedding=e))
        result = service.evaluate(frames, faces)
        assert result.is_live is False
        assert "inconsistentes" in result.reason

    def test_evaluate_signals_poblados(self):
        service = LivenessService()
        e = np.random.default_rng(1).standard_normal(512).astype(np.float32)
        e /= np.linalg.norm(e)
        frames = [_rich_frame() for _ in range(3)]
        faces = [_face(embedding=e) for _ in range(3)]
        r = service.evaluate(frames, faces)
        assert r.signals.texture_score > 0
        assert 0.0 <= r.signals.fft_score <= 1.0
        assert 0.0 <= r.signals.embedding_consistency <= 1.0
