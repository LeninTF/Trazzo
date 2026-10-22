import logging
from typing import Callable
import numpy as np

from config import SETTINGS
from domain.errors import BiometricError, ErrorCode
from domain.models import MatchResult
from services.challenge_service import ChallengePrompt

log = logging.getLogger(__name__)


def _cosine(a: np.ndarray, b: np.ndarray) -> float:
    return float(np.dot(a, b))


class RecognitionService:
    def __init__(self, capture, repo) -> None:
        self.capture = capture
        self.repo = repo

    def _other_model_hint(self, tenant_id: str) -> str | None:
        """Si hay registros creados con otro modelo, lo indica: requieren re-enrolar."""
        counter = getattr(self.repo, "count_other_models", None)
        if not callable(counter):
            return None
        try:
            others = int(counter(tenant_id))
        except (TypeError, ValueError):
            return None
        if others <= 0:
            return None
        return (f"hay {others} registro(s) creados con otro modelo de reconocimiento; "
                f"vuelva a registrar a esas personas con el modelo actual ({SETTINGS.model_pack})")

    async def verify(self, person_id: str, tenant_id: str,
                     on_prompt: Callable[[ChallengePrompt], None] | None = None) -> tuple[MatchResult, dict]:
        stored = self.repo.load_person(person_id, tenant_id)
        if not stored:
            raise BiometricError(ErrorCode.PERSON_NOT_ENROLLED, self._other_model_hint(tenant_id))

        result = await self.capture.capture_passive(on_prompt=on_prompt)
        comparable = [s for s in stored if s.shape == result.embedding.shape]
        if not comparable:
            log.error("verify: templates de %s con dimension incompatible", person_id)
            raise BiometricError(ErrorCode.PERSON_NOT_ENROLLED)
        best = max(_cosine(result.embedding, s) for s in comparable)

        if best < SETTINGS.recognition_threshold:
            raise BiometricError(ErrorCode.NO_MATCH)

        return (
            MatchResult(person_id=person_id, score=round(best, 4)),
            {"liveness": result.liveness.score, "frames": result.frame_count},
        )

    async def identify(self, tenant_id: str,
                       on_prompt: Callable[[ChallengePrompt], None] | None = None) -> tuple[MatchResult, dict]:
        result = await self.capture.capture_passive(on_prompt=on_prompt)

        # Se rastrean el MEJOR y el SEGUNDO mejor score (streaming, sin cargar toda la
        # galería en memoria). El segundo mejor se usa para el chequeo de margen.
        best_id: str | None = None
        best_score = -2.0
        second_score = -2.0
        empty = True

        for person_id, embedding in self.repo.iter_gallery(tenant_id):
            if embedding.shape != result.embedding.shape:
                log.error("identify: template de %s con dimension incompatible, se ignora", person_id)
                continue
            empty = False
            s = _cosine(result.embedding, embedding)
            if s > best_score:
                second_score = best_score
                best_score, best_id = s, person_id
            elif s > second_score:
                second_score = s

        if empty or best_id is None:
            raise BiometricError(ErrorCode.NO_ENROLLED_FACES, self._other_model_hint(tenant_id))

        if best_score < SETTINGS.recognition_threshold:
            raise BiometricError(ErrorCode.NO_MATCH)

        # Margen: el mejor candidato debe superar CLARAMENTE al segundo. Si dos personas
        # enroladas puntúan casi igual, el match es ambiguo y NO se arriesga una identidad
        # equivocada -> NO_MATCH. (second_score == -2.0 => solo hay 1 persona, sin ambigüedad.)
        if second_score > -1.5 and (best_score - second_score) < SETTINGS.recognition_margin:
            log.warning(
                "identify ambiguo (best=%.3f, second=%.3f, margen<%.3f) -> NO_MATCH",
                best_score, second_score, SETTINGS.recognition_margin,
            )
            raise BiometricError(ErrorCode.NO_MATCH)

        return (
            MatchResult(person_id=best_id, score=round(best_score, 4)),
            {"liveness": result.liveness.score, "frames": result.frame_count},
        )
