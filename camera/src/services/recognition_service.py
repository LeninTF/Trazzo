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

    async def verify(self, person_id: str, tenant_id: str,
                     on_prompt: Callable[[ChallengePrompt], None] | None = None) -> tuple[MatchResult, dict]:
        stored = self.repo.load_person(person_id, tenant_id)
        if not stored:
            raise BiometricError(ErrorCode.PERSON_NOT_ENROLLED)

        result = await self.capture.capture_passive(on_prompt=on_prompt)
        best = max(_cosine(result.embedding, s) for s in stored)

        if best < SETTINGS.recognition_threshold:
            raise BiometricError(ErrorCode.NO_MATCH)

        return (
            MatchResult(person_id=person_id, score=round(best, 4)),
            {"liveness": result.liveness.score, "frames": result.frame_count},
        )

    async def identify(self, tenant_id: str,
                       on_prompt: Callable[[ChallengePrompt], None] | None = None) -> tuple[MatchResult, dict]:
        result = await self.capture.capture_passive(on_prompt=on_prompt)

        best_id: str | None = None
        best_score = SETTINGS.recognition_threshold
        empty = True

        for person_id, embedding in self.repo.iter_gallery(tenant_id):
            empty = False
            s = _cosine(result.embedding, embedding)
            if s > best_score:
                best_id, best_score = person_id, s

        if empty:
            raise BiometricError(ErrorCode.NO_ENROLLED_FACES)
        if best_id is None:
            raise BiometricError(ErrorCode.NO_MATCH)

        return (
            MatchResult(person_id=best_id, score=round(best_score, 4)),
            {"liveness": result.liveness.score, "frames": result.frame_count},
        )
