import gc
import logging
from typing import Callable
import numpy as np

from config import SETTINGS
from domain.errors import BiometricError, ErrorCode
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

                # Todas las muestras deben ser de la MISMA persona: si alguien distinto se
                # pone frente a la cámara entre muestras, el promedio mezclaría dos rostros.
                if samples and float(np.dot(samples[0], r.embedding)) < SETTINGS.identity_min_similarity:
                    raise BiometricError(
                        ErrorCode.IDENTITY_INCONSISTENT,
                        "las muestras del registro no son de la misma persona",
                    )

                samples.append(r.embedding)
                log.info("enroll person=%s muestra %d/%d",
                         sanitize_log(person_id), i + 1, SETTINGS.enrollment_samples)

            stacked = np.stack(samples)
            avg = stacked.mean(axis=0)
            avg /= max(1e-6, float(np.linalg.norm(avg)))
            avg = avg.astype(np.float32)

            # Un rostro no puede quedar registrado bajo dos IDs: si ya coincide con OTRA
            # persona de la galería, se rechaza. Sin este control, la identificación
            # posterior devolvería indistintamente uno u otro ID.
            self._reject_if_enrolled_as_other(avg, person_id, tenant_id)

            self.repo.replace_person(EnrolledFace(person_id, tenant_id, avg))
            return {"personId": person_id, "samples": len(samples)}
        finally:
            samples.clear()
            gc.collect()

    def _reject_if_enrolled_as_other(self, embedding: np.ndarray, person_id: str, tenant_id: str) -> None:
        best_other_id: str | None = None
        best_other_score = -2.0

        for other_id, other_embedding in self.repo.iter_gallery(tenant_id):
            if other_id == person_id:
                continue  # re-enrolar a la misma persona es válido (reemplaza su template)
            if other_embedding.shape != embedding.shape:
                continue
            score = float(np.dot(embedding, other_embedding))
            if score > best_other_score:
                best_other_id, best_other_score = other_id, score

        if best_other_id is not None and best_other_score >= SETTINGS.recognition_threshold:
            log.warning("enroll rechazado: person=%s coincide con %s (score=%.3f)",
                        sanitize_log(person_id), sanitize_log(best_other_id), best_other_score)
            raise BiometricError(
                ErrorCode.FACE_ALREADY_ENROLLED,
                f"este rostro ya esta registrado como '{best_other_id}' (score={best_other_score:.2f})",
            )

    def delete(self, person_id: str, tenant_id: str) -> int:
        return self.repo.delete_person(person_id, tenant_id)
