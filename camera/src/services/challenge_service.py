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
from collections import Counter, deque
from dataclasses import dataclass
from enum import Enum
from typing import Callable, Final

import numpy as np

from config import SETTINGS
from domain.errors import BiometricError, ErrorCode
from domain.models import DetectedFace
from services import face_quality

log = logging.getLogger(__name__)

_YAW_FRONT_MAX: Final[float] = 0.08     # frontal
_YAW_TURN_MIN: Final[float] = 0.18      # girado (enroll)
_STAGE_TIMEOUT_S: Final[float] = 20.0
_STABLE_FRAMES: Final[int] = 3
_MAX_SPOOF_STREAK: Final[int] = 30      # rechazos seguidos antes de abortar (~1.5s a 20fps)
_CNN_FUSION_WINDOW: Final[int] = 10     # frames recientes cuya mediana debe superar el umbral

# passive mode (verify/identify): sin prompts, deteccion silenciosa
_PASSIVE_TARGET_FRAMES: Final[int] = 8
_PASSIVE_MIN_FRAMES: Final[int] = 5
_PASSIVE_TIMEOUT_S: Final[float] = 5.0
_PASSIVE_MIN_MOTION: Final[float] = 0.4   # motion_variance minimo
_PASSIVE_MIN_LM_INDEP: Final[float] = 0.08 # landmark_independence minimo
# similitud minima entre cualquier par de frames: SETTINGS.identity_min_similarity


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


def _fused_cnn_score(scores: list[float]) -> float:
    """Fusión temporal del anti-spoof: mediana de TODOS los frames evaluados, aceptados o no.

    Decidir solo con frames sueltos deja pasar un ataque intermitente: si alterna frames
    aceptados y rechazados nunca completa la racha de rechazos y suma los frames que necesita.
    La mediana exige que la mayoría de los frames parezcan reales.
    """
    return float(np.median(scores)) if scores else 0.0


def _lighting_hint(frame: np.ndarray, face: DetectedFace) -> str:
    """Sufijo para el mensaje de rechazo cuando la iluminación explica la duda del CNN."""
    return f"; {face_quality.POOR_LIGHTING_HINT}" if face_quality.lighting(frame, face).poor else ""


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
        quality_issues: Counter[str] = Counter()
        last_issue: str | None = None

        while len(faces) < _PASSIVE_TARGET_FRAMES and time.monotonic() < deadline:
            pair = await self._read_face_with_frame()
            if pair is None:
                await asyncio.sleep(0.05)
                continue
            face, frame = pair

            # Calidad primero (barato): frames de perfil, borrosos o cortados no se usan para
            # reconocer. Se indica al usuario qué corregir.
            quality = face_quality.assess(frame, face)
            if not quality.ok:
                quality_issues[quality.issue] += 1
                if quality.issue != last_issue:
                    self.on_prompt(ChallengePrompt(Stage.LOOK_FRONT_A, None, quality.issue))
                    last_issue = quality.issue
                await asyncio.sleep(0.05)
                continue

            score = await asyncio.to_thread(self.anti_spoof.score, frame, face)
            spoof_scores.append(score)
            if score < SETTINGS.anti_spoof_min_real:
                spoof_streak += 1
                if spoof_streak >= _MAX_SPOOF_STREAK:
                    raise BiometricError(
                        ErrorCode.SPOOF_DETECTED,
                        f"CNN rechazo persistente (real={score:.2f}){_lighting_hint(frame, face)}",
                    )
                await asyncio.sleep(0.05)
                continue
            spoof_streak = 0
            faces.append(face)

        if len(faces) < _PASSIVE_MIN_FRAMES:
            # Si lo que faltó fue calidad (y no el CNN), se informa qué corregir en vez de
            # reportar un falso "spoof".
            if quality_issues and sum(quality_issues.values()) >= len(spoof_scores):
                issue, _ = quality_issues.most_common(1)[0]
                raise BiometricError(ErrorCode.FACE_LOW_QUALITY, f"imagen sin calidad suficiente: {issue}")
            worst = min(spoof_scores) if spoof_scores else 0.0
            raise BiometricError(
                ErrorCode.SPOOF_DETECTED,
                f"pocos frames validos (worst CNN={worst:.2f})",
            )

        fused_cnn = _fused_cnn_score(spoof_scores)
        if fused_cnn < SETTINGS.anti_spoof_min_real:
            raise BiometricError(
                ErrorCode.SPOOF_DETECTED,
                f"CNN fusionado bajo (mediana={fused_cnn:.2f} en {len(spoof_scores)} frames)"
                f"{_lighting_hint(frame, face)}",
            )

        # Consistencia de identidad: si otra persona entra o sale durante la ventana de
        # captura, promediar esos frames produce un embedding mezclado que puede coincidir
        # con un tercero. Se exige que todos los frames sean del mismo rostro.
        # Se compara entre PARES de frames, no contra el promedio: con una mezcla 50/50 de dos
        # personas cada frame queda a ~0.71 del promedio y pasaría un filtro contra la media.
        embs = np.stack([f.embedding for f in faces])
        pairwise = embs @ embs.T
        min_identity = float(pairwise[~np.eye(len(faces), dtype=bool)].min())
        mean = embs.mean(axis=0)
        mean /= max(1e-6, float(np.linalg.norm(mean)))
        if min_identity < SETTINGS.identity_min_similarity:
            raise BiometricError(
                ErrorCode.IDENTITY_INCONSISTENT,
                f"los frames no son de la misma persona (min_sim={min_identity:.2f})",
            )

        motion = _motion_variance(faces)
        lm_indep = _landmark_independence(faces)
        log.info("passive metrics: frames=%d identity_min=%.2f motion=%.2f lm_indep=%.2f "
                 "cnn_median=%.2f cnn_min=%.2f",
                 len(faces), min_identity, motion, lm_indep, fused_cnn, min(spoof_scores))

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

        self.on_prompt(ChallengePrompt(Stage.DONE, None, "listo"))
        return mean.astype(np.float32), {
            "mode": "passive",
            "frames": len(faces),
            "motion": round(motion, 3),
            "cnn_min": round(min(spoof_scores), 3),
            "cnn_median": round(fused_cnn, 3),
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
        stable_faces: list[DetectedFace] = []
        last_issue: str | None = None
        # últimos puntajes del CNN en la etapa, para la fusión temporal (ver _fused_cnn_score)
        recent_cnn: deque[float] = deque(maxlen=_CNN_FUSION_WINDOW)

        while time.monotonic() < deadline:
            face_frame = await self._read_face_with_frame()
            if face_frame is None:
                stable = 0
                stable_faces.clear()
                await asyncio.sleep(0.05)
                continue

            face, frame = face_frame

            # En etapas que capturan el embedding (frontales), solo sirven frames de calidad.
            if capture:
                quality = face_quality.assess(frame, face)
                if not quality.ok:
                    stable = 0
                    stable_faces.clear()
                    if quality.issue != last_issue:
                        self.on_prompt(ChallengePrompt(stage, direction, quality.issue))
                        last_issue = quality.issue
                    await asyncio.sleep(0.05)
                    continue
                if last_issue is not None:
                    self.on_prompt(ChallengePrompt(stage, direction, prompt_text))
                    last_issue = None

            # CNN anti-spoof: usa el MISMO frame donde se detecto el rostro.
            if require_cnn:
                score = await asyncio.to_thread(self.anti_spoof.score, frame, face)
                recent_cnn.append(score)
                if score < SETTINGS.anti_spoof_min_real:
                    spoof_streak += 1
                    stable = 0
                    log.debug("stage=%s spoof frame (score=%.2f, streak=%d)",
                              stage.value, score, spoof_streak)
                    if spoof_streak >= _MAX_SPOOF_STREAK:
                        raise BiometricError(
                            ErrorCode.SPOOF_DETECTED,
                            f"CNN rechazo persistente en {stage.value} (real={score:.2f})"
                            f"{_lighting_hint(frame, face)}",
                        )
                    stable_faces.clear()
                    await asyncio.sleep(0.05)
                    continue
                spoof_streak = 0

            yaw = estimate_yaw_ratio(face)
            if cond(yaw):
                stable += 1
                stable_faces.append(face)
                if stable >= _STABLE_FRAMES:
                    if require_cnn:
                        fused = _fused_cnn_score(list(recent_cnn))
                        if fused < SETTINGS.anti_spoof_min_real:
                            raise BiometricError(
                                ErrorCode.SPOOF_DETECTED,
                                f"CNN fusionado bajo en {stage.value} (mediana={fused:.2f} "
                                f"en {len(recent_cnn)} frames){_lighting_hint(frame, face)}",
                            )
                    if not capture:
                        return np.array([], dtype=np.float32), yaw
                    # Promedio de TODOS los frames estables (no solo el último): un template
                    # más robusto frente al ruido de un único frame.
                    embs = np.stack([f.embedding for f in stable_faces[-_STABLE_FRAMES:]])
                    emb = embs.mean(axis=0)
                    emb /= max(1e-6, float(np.linalg.norm(emb)))
                    return emb.astype(np.float32), yaw
            else:
                stable = 0
                stable_faces.clear()

            await asyncio.sleep(0.05)

        if capture and last_issue is not None:
            raise BiometricError(
                ErrorCode.FACE_LOW_QUALITY,
                f"no se logro una imagen de calidad en '{stage.value}': {last_issue}",
            )
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
