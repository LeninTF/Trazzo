import gc
import logging
from typing import Callable

from domain.models import CaptureResult, LivenessResult, LivenessSignals
from services.challenge_service import ActiveLivenessSession, ChallengePrompt

log = logging.getLogger(__name__)


class CaptureService:
    """Dos flujos de captura con active liveness:

    - full  (enrollment):  frontal -> gira izq/der -> vuelve al frente.
                           Robusto, one-time.
    - quick (verify/id):   frontal + pequeno movimiento en ~3s.
                           Rapido para uso diario, sigue detectando fotos.
    """

    def __init__(self, camera, engine, liveness, anti_spoof) -> None:
        self.camera = camera
        self.engine = engine
        self.liveness = liveness   # conservado para compatibilidad
        self.anti_spoof = anti_spoof

    async def capture_full(self, on_prompt: Callable[[ChallengePrompt], None] | None = None) -> CaptureResult:
        session = ActiveLivenessSession(self.camera, self.engine, self.anti_spoof, on_prompt)
        try:
            embedding, meta = await session.run_full()
            return self._to_result(embedding, meta, frames=2)
        finally:
            gc.collect()

    async def capture_passive(self, on_prompt: Callable[[ChallengePrompt], None] | None = None) -> CaptureResult:
        session = ActiveLivenessSession(self.camera, self.engine, self.anti_spoof, on_prompt)
        try:
            embedding, meta = await session.run_passive()
            return self._to_result(embedding, meta, frames=meta.get("frames", 1))
        finally:
            gc.collect()

    # alias por compatibilidad hacia atras
    async def capture_quick(self, on_prompt: Callable[[ChallengePrompt], None] | None = None) -> CaptureResult:
        return await self.capture_passive(on_prompt)

    # alias por compatibilidad — enrollment sigue llamando capture(), por defecto full
    async def capture(self, on_prompt: Callable[[ChallengePrompt], None] | None = None) -> CaptureResult:
        return await self.capture_full(on_prompt)

    @staticmethod
    def _to_result(embedding, meta: dict, frames: int) -> CaptureResult:
        liveness = LivenessResult(
            is_live=True,
            score=1.0,
            signals=LivenessSignals(0, 0, 0, meta.get("match_between_frontals", 1.0), 0, 0),
            reason=f"challenge {meta['mode']} completado",
        )
        return CaptureResult(
            embedding=embedding,
            liveness=liveness,
            frame_count=frames,
            face_size=0,
        )
