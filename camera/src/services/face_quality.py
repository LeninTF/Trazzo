"""Control de calidad de cada rostro antes de usarlo para enrolar o reconocer.

Un embedding sale tan bueno como la imagen de la que se extrae. Caras de perfil, borrosas,
cortadas por el borde o mal detectadas producen embeddings ruidosos que acercan a personas
distintas entre sí (falsos positivos) o alejan a la persona correcta (falsos negativos).
Por eso solo se aceptan frames que pasen todos estos criterios.
"""
from dataclasses import dataclass
from typing import Final

import cv2
import numpy as np

from config import SETTINGS
from domain.models import DetectedFace

# tamaño fijo al que se normaliza el recorte para medir nitidez: así el umbral no depende de
# la resolución de la cámara ni de la distancia de la persona
_SHARPNESS_SIZE: Final[int] = 112
# margen mínimo (fracción del tamaño de la cara) entre la cara y el borde del frame
_EDGE_MARGIN_RATIO: Final[float] = 0.05


class QualityIssue:
    LOW_DETECTION = "deteccion poco confiable"
    TOO_SMALL = "acercate mas a la camara"
    CUT_OFF = "centra tu rostro en la camara"
    NOT_FRONTAL = "mira de frente a la camara"
    BLURRY = "imagen borrosa, quedate quieto"


@dataclass(frozen=True)
class FaceQuality:
    ok: bool
    issue: str | None
    detection_score: float
    size: int
    yaw: float
    pitch: float
    sharpness: float


def yaw_ratio(face: DetectedFace) -> float:
    """Giro horizontal: desplazamiento de la nariz respecto al centro de los ojos, normalizado
    por la distancia entre ojos. ~0 de frente; crece en valor absoluto al girar."""
    kps = face.keypoints
    eye_center_x = (kps[0][0] + kps[1][0]) / 2
    eye_dist = max(1.0, float(kps[1][0] - kps[0][0]))
    return float((kps[2][0] - eye_center_x) / eye_dist)


def pitch_ratio(face: DetectedFace) -> float:
    """Inclinación vertical: posición de la nariz entre la línea de los ojos (0) y la de la boca
    (1). De frente ronda ~0.5; mirando arriba/abajo se acerca a los extremos."""
    kps = face.keypoints
    eye_y = (kps[0][1] + kps[1][1]) / 2
    mouth_y = (kps[3][1] + kps[4][1]) / 2
    span = float(mouth_y - eye_y)
    if span <= 1.0:
        return 0.0  # landmarks degenerados: se trata como no frontal
    return float((kps[2][1] - eye_y) / span)


def sharpness(frame: np.ndarray, face: DetectedFace) -> float:
    """Varianza del Laplaciano sobre el recorte de la cara normalizado a 112x112."""
    h, w = frame.shape[:2]
    x1 = max(0, int(face.bbox[0]))
    y1 = max(0, int(face.bbox[1]))
    x2 = min(w, int(face.bbox[2]))
    y2 = min(h, int(face.bbox[3]))
    if x2 - x1 < 8 or y2 - y1 < 8:
        return 0.0
    crop = cv2.cvtColor(frame[y1:y2, x1:x2], cv2.COLOR_BGR2GRAY)
    crop = cv2.resize(crop, (_SHARPNESS_SIZE, _SHARPNESS_SIZE), interpolation=cv2.INTER_AREA)
    return float(cv2.Laplacian(crop, cv2.CV_64F).var())


# Iluminación del rostro (HSV sobre el recorte). Con poca luz la webcam sube la ganancia y la
# exposición: la imagen sale gris, con poco contraste y ruido, justo el aspecto de una foto
# impresa para el CNN anti-spoof. Medido en este equipo con luz pobre: saturación 37,
# contraste 33 y score anti-spoof ~0.0 aun siendo una persona real; con buena luz, ~0.9.
_LIGHT_MIN_BRIGHTNESS: Final[float] = 70.0
_LIGHT_MAX_BRIGHTNESS: Final[float] = 215.0
_LIGHT_MIN_SATURATION: Final[float] = 45.0
_LIGHT_MIN_CONTRAST: Final[float] = 35.0
POOR_LIGHTING_HINT: Final[str] = "poca luz o imagen apagada: ilumina tu rostro de frente"


@dataclass(frozen=True)
class Lighting:
    brightness: float
    saturation: float
    contrast: float

    @property
    def poor(self) -> bool:
        return (not _LIGHT_MIN_BRIGHTNESS <= self.brightness <= _LIGHT_MAX_BRIGHTNESS
                or self.saturation < _LIGHT_MIN_SATURATION
                or self.contrast < _LIGHT_MIN_CONTRAST)


def lighting(frame: np.ndarray, face: DetectedFace) -> Lighting:
    """Brillo, saturación y contraste medios del rostro. Informativo: explica por qué el
    anti-spoof puede dudar; no se usa para aceptar frames (eso lo decide el CNN)."""
    h, w = frame.shape[:2]
    x1, y1 = max(0, int(face.bbox[0])), max(0, int(face.bbox[1]))
    x2, y2 = min(w, int(face.bbox[2])), min(h, int(face.bbox[3]))
    if x2 - x1 < 8 or y2 - y1 < 8:
        return Lighting(0.0, 0.0, 0.0)
    hsv = cv2.cvtColor(frame[y1:y2, x1:x2], cv2.COLOR_BGR2HSV)
    return Lighting(float(hsv[:, :, 2].mean()), float(hsv[:, :, 1].mean()), float(hsv[:, :, 2].std()))


def assess(frame: np.ndarray, face: DetectedFace) -> FaceQuality:
    yaw = yaw_ratio(face)
    pitch = pitch_ratio(face)
    sharp = sharpness(frame, face)

    def result(issue: str | None) -> FaceQuality:
        return FaceQuality(issue is None, issue, face.detection_score, face.size, yaw, pitch, sharp)

    # Orden: primero las causas que la persona puede corregir. Una cara cortada por el borde
    # también baja la confianza del detector; decir "centra tu rostro" es accionable,
    # "detección poco confiable" no.
    if face.size < SETTINGS.min_face_size:
        return result(QualityIssue.TOO_SMALL)

    h, w = frame.shape[:2]
    margin = face.size * _EDGE_MARGIN_RATIO
    x1, y1, x2, y2 = (float(v) for v in face.bbox)
    if x1 < margin or y1 < margin or x2 > w - margin or y2 > h - margin:
        return result(QualityIssue.CUT_OFF)

    if face.detection_score < SETTINGS.quality_min_detection_score:
        return result(QualityIssue.LOW_DETECTION)

    if abs(yaw) > SETTINGS.quality_max_yaw or not (
            SETTINGS.quality_min_pitch <= pitch <= SETTINGS.quality_max_pitch):
        return result(QualityIssue.NOT_FRONTAL)

    if sharp < SETTINGS.quality_min_sharpness:
        return result(QualityIssue.BLURRY)

    return result(None)
