from typing import Final

import numpy as np
import cv2

from config import SETTINGS
from domain.models import DetectedFace, LivenessResult, LivenessSignals

# gates duros — cualquiera de estos por debajo => rechazo directo
_MIN_CONSISTENCY: Final[float] = 0.75
_MIN_MOTION_VARIANCE: Final[float] = 0.8   # micro-motion natural de un humano
_MIN_LANDMARK_INDEP: Final[float] = 0.15   # ojos/boca deben moverse relativamente
_MAX_FFT: Final[float] = 0.65              # arriba de esto es sospechoso (screen sharpening)

# normalizaciones para score suave
_TEXTURE_NORM: Final[float] = 120.0
_FFT_MIN: Final[float] = 0.25
_FFT_RANGE: Final[float] = 0.25
_SKIN_MIN: Final[float] = 0.15
_SKIN_RANGE: Final[float] = 0.35
_FFT_INPUT_SIZE: Final[int] = 128
_FFT_LOW_RADIUS: Final[int] = 20
_FFT_HIGH_RADIUS: Final[int] = 40

_W_TEXTURE: Final[float] = 0.25
_W_FFT: Final[float] = 0.25
_W_SKIN: Final[float] = 0.15
_W_CONSISTENCY: Final[float] = 0.10
_W_MOTION: Final[float] = 0.25


def _crop(frame: np.ndarray, face: DetectedFace) -> np.ndarray:
    h, w = frame.shape[:2]
    x1 = max(0, int(face.bbox[0]))
    y1 = max(0, int(face.bbox[1]))
    x2 = min(w, int(face.bbox[2]))
    y2 = min(h, int(face.bbox[3]))
    if x2 <= x1 or y2 <= y1:
        return frame
    return frame[y1:y2, x1:x2]


def _lap_var(face_crop: np.ndarray) -> float:
    gray = cv2.cvtColor(face_crop, cv2.COLOR_BGR2GRAY)
    return float(cv2.Laplacian(gray, cv2.CV_64F).var())


def _fft_high_freq_ratio(face_crop: np.ndarray) -> float:
    gray = cv2.cvtColor(face_crop, cv2.COLOR_BGR2GRAY)
    gray = cv2.resize(gray, (_FFT_INPUT_SIZE, _FFT_INPUT_SIZE))
    magnitude = np.abs(np.fft.fftshift(np.fft.fft2(gray)))

    h, w = magnitude.shape
    cy, cx = h // 2, w // 2
    y, x = np.ogrid[:h, :w]
    dist = np.sqrt((x - cx) ** 2 + (y - cy) ** 2)

    low = magnitude[dist <= _FFT_LOW_RADIUS].sum()
    high = magnitude[dist > _FFT_HIGH_RADIUS].sum()
    total = low + high
    if total < 1e-6:
        return 0.0
    return float(high / total)


def _skin_ratio(face_crop: np.ndarray) -> float:
    hsv = cv2.cvtColor(face_crop, cv2.COLOR_BGR2HSV)
    lower1 = np.array([0, 30, 60], dtype=np.uint8)
    upper1 = np.array([25, 170, 255], dtype=np.uint8)
    lower2 = np.array([160, 30, 60], dtype=np.uint8)
    upper2 = np.array([180, 170, 255], dtype=np.uint8)
    mask = cv2.inRange(hsv, lower1, upper1) | cv2.inRange(hsv, lower2, upper2)
    return float(mask.mean() / 255.0)


def _consistency(faces: list[DetectedFace]) -> float:
    if len(faces) < 2:
        return 1.0
    embs = np.stack([f.embedding for f in faces])
    mean = embs.mean(axis=0)
    mean /= max(1e-6, float(np.linalg.norm(mean)))
    return float((embs @ mean).mean())


def _motion_variance(faces: list[DetectedFace]) -> float:
    """Varianza combinada de posicion + tamano del bbox normalizada por tamano.

    Foto sostenida: variance cercana a 0.
    Cara real: variance > 1.0 tipicamente (respiracion, latido, micro-movimiento).
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
    """Mide si los landmarks se mueven independientes entre si.

    Foto: si toda la imagen se mueve, ojo_L, ojo_R, nariz se mueven en bloque
    (correlacion casi 1). Cara real: cada landmark tiene micro-movimiento
    independiente (correlacion menor).

    Devuelve varianza de las diferencias relativas — mas alto = mas independiente.
    """
    if len(faces) < 3:
        return 0.0

    kps = np.stack([f.keypoints for f in faces])  # (N, 5, 2)
    # normalizar por tamano del rostro para que sea distancia-invariante
    sizes = np.array([f.size for f in faces], dtype=np.float32).reshape(-1, 1, 1)
    kps_norm = kps / np.maximum(sizes, 1.0)

    # vectores relativos: ojo_izq -> ojo_der, ojo_izq -> nariz, ojo_izq -> boca_izq
    v1 = kps_norm[:, 1] - kps_norm[:, 0]   # ojo derecho relativo al izquierdo
    v2 = kps_norm[:, 2] - kps_norm[:, 0]   # nariz relativa al ojo izquierdo
    v3 = kps_norm[:, 3] - kps_norm[:, 0]   # boca_izq relativa al ojo izquierdo

    return float(v1.std() + v2.std() + v3.std()) * 100


class LivenessService:
    """Anti-spoof pasivo. Capa 1 = CNN entrenada. Esta es capa 2 (heuristicas)."""

    def evaluate(self, frames: list[np.ndarray], faces: list[DetectedFace]) -> LivenessResult:
        if not frames or not faces:
            return LivenessResult(
                False, 0.0,
                LivenessSignals(0, 0, 0, 0, 0, 0),
                "sin frames",
            )

        crops = [_crop(fr, fc) for fr, fc in zip(frames, faces)]
        texture = float(np.mean([_lap_var(c) for c in crops]))
        fft_hf = float(np.mean([_fft_high_freq_ratio(c) for c in crops]))
        skin = float(np.mean([_skin_ratio(c) for c in crops]))
        consistency = _consistency(faces)
        motion = _motion_variance(faces)
        landmark_indep = _landmark_independence(faces)

        signals = LivenessSignals(
            texture_score=texture,
            fft_score=fft_hf,
            skin_score=skin,
            embedding_consistency=consistency,
            motion_variance=motion,
            landmark_independence=landmark_indep,
        )

        # gates duros (ordenados de mas restrictivo a menos)
        if consistency < _MIN_CONSISTENCY:
            return LivenessResult(False, 0.0, signals, "rostros inconsistentes entre frames")
        if motion < _MIN_MOTION_VARIANCE:
            return LivenessResult(False, 0.0, signals,
                                  f"sin micro-movimiento (motion={motion:.2f}, min={_MIN_MOTION_VARIANCE})")
        if landmark_indep < _MIN_LANDMARK_INDEP:
            return LivenessResult(False, 0.0, signals,
                                  "landmarks se mueven en bloque (imagen 2D sostenida)")
        if fft_hf > _MAX_FFT:
            return LivenessResult(False, 0.0, signals,
                                  "alta frecuencia excesiva (posible pantalla con sharpening)")

        t = min(1.0, texture / _TEXTURE_NORM)
        f = min(1.0, max(0.0, (fft_hf - _FFT_MIN) / _FFT_RANGE))
        s = min(1.0, max(0.0, (skin - _SKIN_MIN) / _SKIN_RANGE))
        c = max(0.0, (consistency - _MIN_CONSISTENCY) / (1.0 - _MIN_CONSISTENCY))
        m = min(1.0, motion / 4.0)  # 4.0 = mucho movimiento natural

        score = (_W_TEXTURE * t + _W_FFT * f + _W_SKIN * s
                 + _W_CONSISTENCY * c + _W_MOTION * m)

        if score < SETTINGS.liveness_min_score:
            return LivenessResult(False, round(score, 4), signals,
                                  f"score bajo (t={t:.2f} f={f:.2f} s={s:.2f} m={m:.2f})")
        return LivenessResult(True, round(score, 4), signals)
