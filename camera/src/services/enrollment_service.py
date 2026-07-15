import gc
import logging
from typing import Callable
import numpy as np

from config import SETTINGS
from domain.models import EnrolledFace
from domain.validators import sanitize_log
from services.challenge_service import ChallengePrompt

log = logging.getLogger(__name__)


class EnrollmentService:
    def __init__(self, capture, repo) -> None:
        self.capture = capture
        self.repo = repo

    async def enroll(self, person_id: str, tenant_id: str,
                     on_prompt: Callable[[ChallengePrompt], None] | None = None) -> dict:
        samples: list[np.ndarray] = []
        try:
            for i in range(SETTINGS.enrollment_samples):
                r = await self.capture.capture(on_prompt=on_prompt)
                samples.append(r.embedding)
                log.info("enroll person=%s muestra %d/%d",
                         sanitize_log(person_id), i + 1, SETTINGS.enrollment_samples)

            stacked = np.stack(samples)
            avg = stacked.mean(axis=0)
            avg /= max(1e-6, float(np.linalg.norm(avg)))

            self.repo.replace_person(EnrolledFace(person_id, tenant_id, avg.astype(np.float32)))
            return {"personId": person_id, "samples": len(samples)}
        finally:
            samples.clear()
            gc.collect()

    def delete(self, person_id: str, tenant_id: str) -> int:
        return self.repo.delete_person(person_id, tenant_id)
