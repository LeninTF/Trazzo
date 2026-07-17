"""Active liveness — challenge-response al estilo iProov/FaceTec.

Modos:
  - full  (enrollment): mira al frente -> gira izq/der (aleatorio) -> vuelve al frente
  - quick (verify/id):  mira al frente + pequeno movimiento

En AMBOS, cada frame usado se valida contra el CNN anti-spoof.
"""
import time
import asyncio
import random
import logging
from dataclasses import dataclass
from enum import Enum
from typing import Callable, Final

import numpy as np

from config import SETTINGS
from domain.errors import BiometricError, ErrorCode
from domain.models import DetectedFace

log = logging.getLogger(__name__)

_YAW_FRONT_MAX: Final[float] = 0.08     # frontal
_YAW_TURN_MIN: Final[float] = 0.18      # girado (enroll)
_STAGE_TIMEOUT_S: Final[float] = 20.0
_STABLE_FRAMES: Final[int] = 3
_MAX_SPOOF_STREAK: Final[int] = 30      # rechazos seguidos antes de abortar (~1.5s a 20fps)

# passive mode (verify/identify): sin prompts, deteccion silenciosa
_PASSIVE_TARGET_FRAMES: Final[int] = 8
_PASSIVE_MIN_FRAMES: Final[int] = 5
_PASSIVE_TIMEOUT_S: Final[float] = 5.0
_PASSIVE_MIN_MOTION: Final[float] = 0.4   # motion_variance minimo
_PASSIVE_MIN_LM_INDEP: Final[float] = 0.08 # landmark_independence minimo


class Direction(str, Enum):
    LEFT = "izquierda"
    RIGHT = "derecha"


class Stage(str, Enum):
    LOOK_FRONT_A = "mira al frente"
    TURN = "gira la cabeza"
    LOOK_FRONT_B = "vuelve al frente"
    DONE = "listo"


@dataclass(frozen=True)
class ChallengePrompt:
    stage: Stage
    direction: Direction | None
    text: str


def estimate_yaw_ratio(face: DetectedFace) -> float:
    kps = face.keypoints
    eye_center_x = (kps[0][0] + kps[1][0]) / 2
    nose_x = kps[2][0]
    face_w = max(1.0, float(kps[1][0] - kps[0][0]))
    return float((nose_x - eye_center_x) / face_w)


def _motion_variance(faces: list[DetectedFace]) -> float:
    """Varianza de posicion + tamano del bbox normalizada por tamano.
    Foto quieta ~0. Cara real >0.4 (respiracion, latido).
    """
    if len(faces) < 3:
        return 0.0
    centers = np.array([[(f.bbox[0] + f.bbox[2]) / 2,
                         (f.bbox[1] + f.bbox[3]) / 2] for f in faces])
    sizes = np.array([f.size for f in faces], dtype=np.float32)
    mean_size = max(1.0, float(sizes.mean()))
    pos_std = float(np.linalg.norm(centers.std(axis=0))) / mean_size * 100
    size_std = float(sizes.std()) / mean_size * 100
    return pos_std + size_std


def _landmark_independence(faces: list[DetectedFace]) -> float:
    """Movimiento relativo de landmarks. Foto=bloque rigido, cara=independientes."""
    if len(faces) < 3:
        return 0.0
    kps = np.stack([f.keypoints for f in faces])
    sizes = np.array([f.size for f in faces], dtype=np.float32).reshape(-1, 1, 1)
    kps_norm = kps / np.maximum(sizes, 1.0)
    v1 = kps_norm[:, 1] - kps_norm[:, 0]
    v2 = kps_norm[:, 2] - kps_norm[:, 0]
    v3 = kps_norm[:, 3] - kps_norm[:, 0]
    return float(v1.std() + v2.std() + v3.std()) * 100


class ActiveLivenessSession:
    def __init__(self, camera, engine, anti_spoof,
                 on_prompt: Callable[[ChallengePrompt], None] | None = None):
        self.camera = camera
        self.engine = engine
        self.anti_spoof = anti_spoof
        self.on_prompt = on_prompt or (lambda _: None)

    # ------------------------- flujos publicos -------------------------

    async def run_full(self) -> tuple[np.ndarray, dict]:
        direction = random.SystemRandom().choice([Direction.LEFT, Direction.RIGHT])
        log.info("challenge FULL iniciado direccion=%s", direction.value)

        emb_a, _ = await self._wait_for(
            stage=Stage.LOOK_FRONT_A,
            cond=lambda y: abs(y) < _YAW_FRONT_MAX,
            require_cnn=True,
        )
        await self._wait_for(
            stage=Stage.TURN,
            cond=(lambda y: y > _YAW_TURN_MIN) if direction == Direction.LEFT
                 else (lambda y: y < -_YAW_TURN_MIN),
            require_cnn=False,   # perfil suele confundir al CNN
            direction=direction,
            capture=False,
        )
        emb_b, _ = await self._wait_for(
            stage=Stage.LOOK_FRONT_B,
            cond=lambda y: abs(y) < _YAW_FRONT_MAX,
            require_cnn=True,
        )

        similarity = float(np.dot(emb_a, emb_b))
        if similarity < 0.75:
            raise BiometricError(
                ErrorCode.SPOOF_DETECTED,
                f"rostro cambio entre frontales (sim={similarity:.2f})",
            )

        mean = (emb_a + emb_b) / 2
        mean /= max(1e-6, float(np.linalg.norm(mean)))

        self.on_prompt(ChallengePrompt(Stage.DONE, None, "listo"))
        return mean.astype(np.float32), {
            "mode": "full",
            "direction": direction.value,
            "match_between_frontals": round(similarity, 4),
        }

    async def run_passive(self) -> tuple[np.ndarray, dict]:
        """Verify/identify: sin prompts. Captura N frames en ventana corta,
        cada frame validado por CNN. Al final chequea motion y landmark
        independence entre los frames — una foto plana falla ambos.
        """
        log.info("passive liveness iniciado")
        self.on_prompt(ChallengePrompt(Stage.LOOK_FRONT_A, None, "mira a la camara"))

        deadline = time.monotonic() + _PASSIVE_TIMEOUT_S
        faces: list[DetectedFace] = []
        spoof_scores: list[float] = []
        spoof_streak = 0

        while len(faces) < _PASSIVE_TARGET_FRAMES and time.monotonic() < deadline:
            pair = await self._read_face_with_frame()
            if pair is None:
                await asyncio.sleep(0.05)
                continue
            face, frame = pair

            score = await asyncio.to_thread(self.anti_spoof.score, frame, face)
            spoof_scores.append(score)
            if score < SETTINGS.anti_spoof_min_real:
                spoof_streak += 1
                if spoof_streak >= _MAX_SPOOF_STREAK:
                    raise BiometricError(
                        ErrorCode.SPOOF_DETECTED,
                        f"CNN rechazo persistente (real={score:.2f})",
                    )
                await asyncio.sleep(0.05)
                continue
            spoof_streak = 0
            faces.append(face)

        if len(faces) < _PASSIVE_MIN_FRAMES:
            worst = min(spoof_scores) if spoof_scores else 0.0
            raise BiometricError(
                ErrorCode.SPOOF_DETECTED,
                f"pocos frames validos (worst CNN={worst:.2f})",
            )

        motion = _motion_variance(faces)
        lm_indep = _landmark_independence(faces)
        log.info("passive metrics: frames=%d motion=%.2f lm_indep=%.2f cnn_min=%.2f",
                 len(faces), motion, lm_indep, min(spoof_scores))

        if motion < _PASSIVE_MIN_MOTION:
            raise BiometricError(
                ErrorCode.SPOOF_DETECTED,
                f"rostro demasiado estatico (motion={motion:.2f}, min={_PASSIVE_MIN_MOTION})",
            )
        if lm_indep < _PASSIVE_MIN_LM_INDEP:
            raise BiometricError(
                ErrorCode.SPOOF_DETECTED,
                "landmarks se mueven en bloque (imagen 2D detectada)",
            )

        embs = np.stack([f.embedding for f in faces])
        mean = embs.mean(axis=0)
        mean /= max(1e-6, float(np.linalg.norm(mean)))

        self.on_prompt(ChallengePrompt(Stage.DONE, None, "listo"))
        return mean.astype(np.float32), {
            "mode": "passive",
            "frames": len(faces),
            "motion": round(motion, 3),
            "cnn_min": round(min(spoof_scores), 3),
        }

    # ------------------------- loop interno -------------------------

    async def _wait_for(self, *,
                        stage: Stage,
                        cond: Callable[[float], bool],
                        require_cnn: bool,
                        direction: Direction | None = None,
                        capture: bool = True) -> tuple[np.ndarray, float]:
        """Bucle unico: lee frames, detecta rostro, valida CNN, chequea condicion.

        Devuelve (embedding, yaw) del frame que cumplio la condicion (embedding
        vacio si capture=False).
        """
        prompt_text = stage.value + (f" a la {direction.value}" if direction else "")
        self.on_prompt(ChallengePrompt(stage, direction, prompt_text))

        deadline = time.monotonic() + _STAGE_TIMEOUT_S
        stable = 0
        spoof_streak = 0
        last_face: DetectedFace | None = None

        while time.monotonic() < deadline:
            face_frame = await self._read_face_with_frame()
            if face_frame is None:
                stable = 0
                await asyncio.sleep(0.05)
                continue

            face, frame = face_frame

            # CNN anti-spoof: usa el MISMO frame donde se detecto el rostro.
            if require_cnn:
                score = await asyncio.to_thread(self.anti_spoof.score, frame, face)
                if score < SETTINGS.anti_spoof_min_real:
                    spoof_streak += 1
                    stable = 0
                    log.debug("stage=%s spoof frame (score=%.2f, streak=%d)",
                              stage.value, score, spoof_streak)
                    if spoof_streak >= _MAX_SPOOF_STREAK:
                        raise BiometricError(
                            ErrorCode.SPOOF_DETECTED,
                            f"CNN rechazo persistente en {stage.value} (real={score:.2f})",
                        )
                    await asyncio.sleep(0.05)
                    continue
                spoof_streak = 0

            yaw = estimate_yaw_ratio(face)
            if cond(yaw):
                stable += 1
                last_face = face
                if stable >= _STABLE_FRAMES:
                    emb = last_face.embedding if capture else np.array([], dtype=np.float32)
                    return emb, yaw
            else:
                stable = 0

            await asyncio.sleep(0.05)

        raise BiometricError(
            ErrorCode.SPOOF_DETECTED,
            f"sesion abandonada en {stage.value}",
        )

    async def _read_face_with_frame(self) -> tuple[DetectedFace, np.ndarray] | None:
        try:
            frame = await asyncio.to_thread(self.camera.read)
        except BiometricError:
            return None
        detected = await asyncio.to_thread(self.engine.detect, frame)
        if len(detected) != 1:
            return None
        face = detected[0]
        if face.size < SETTINGS.min_face_size:
            return None
        return face, frame
