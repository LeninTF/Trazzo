from dataclasses import dataclass, field
import numpy as np


@dataclass(frozen=True)
class DetectedFace:
    bbox: np.ndarray
    keypoints: np.ndarray
    embedding: np.ndarray
    detection_score: float
    pose: np.ndarray | None = None

    @property
    def width(self):
        return int(self.bbox[2] - self.bbox[0])

    @property
    def height(self):
        return int(self.bbox[3] - self.bbox[1])

    @property
    def size(self):
        return min(self.width, self.height)


@dataclass(frozen=True)
class LivenessSignals:
    texture_score: float
    fft_score: float
    skin_score: float
    embedding_consistency: float
    motion_variance: float = 0.0
    landmark_independence: float = 0.0


@dataclass(frozen=True)
class LivenessResult:
    is_live: bool
    score: float
    signals: LivenessSignals
    reason: str | None = None


@dataclass(frozen=True)
class CaptureResult:
    embedding: np.ndarray
    liveness: LivenessResult
    frame_count: int
    face_size: int


@dataclass(frozen=True)
class MatchResult:
    person_id: str
    score: float


@dataclass(frozen=True)
class EnrolledFace:
    person_id: str
    tenant_id: str
    embedding: np.ndarray = field(repr=False)
