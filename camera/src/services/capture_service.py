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
            return self._to_result(embedding, meta, frames=int(meta.get("frames", 1)))
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
        """Construye el CaptureResult con un score de liveness REAL derivado del
        challenge, no un valor fijo.

        - passive (verify/identify): el score lo domina la confianza del CNN
          anti-spoof (probabilidad de "real", `cnn_min`), respaldada por el
          micro-movimiento medido (`motion`).
        - full (enrollment): el score es la similitud del rostro entre los dos
          frontales del reto (`match_between_frontals`); ambos frontales ya
          pasaron el CNN durante el reto.
        """
        mode = meta.get("mode", "passive")

        if mode == "passive":
            cnn = float(meta.get("cnn_min", 1.0))
            motion = float(meta.get("motion", 0.0))
            score = round(max(0.0, min(1.0, cnn)), 4)
            signals = LivenessSignals(0, 0, 0, 1.0, motion, 0)
            reason = f"passive liveness ok (cnn={cnn:.2f}, motion={motion:.2f})"
        else:
            sim = float(meta.get("match_between_frontals", 1.0))
            score = round(max(0.0, min(1.0, sim)), 4)
            signals = LivenessSignals(0, 0, 0, sim, 0, 0)
            reason = f"challenge full ok (match={sim:.2f})"

        liveness = LivenessResult(
            is_live=True,
            score=score,
            signals=signals,
            reason=reason,
        )
        return CaptureResult(
            embedding=embedding,
            liveness=liveness,
            frame_count=frames,
            face_size=0,
        )
