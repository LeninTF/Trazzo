import logging
import threading
import numpy as np

# NOTA: config debe importarse primero — setea OMP_NUM_THREADS antes de onnxruntime
from config import SETTINGS
from insightface.app import FaceAnalysis

from domain.models import DetectedFace

log = logging.getLogger(__name__)


class FaceEngine:
    _instance = None
    _lock = threading.Lock()

    def __init__(self):
        self._app = FaceAnalysis(
            name=SETTINGS.model_pack,
            root=SETTINGS.models_dir,
            providers=["CPUExecutionProvider"],
        )
        self._app.prepare(ctx_id=0, det_thresh=SETTINGS.detection_threshold, det_size=(640, 640))
        log.info("engine listo (pack=%s)", SETTINGS.model_pack)

    @classmethod
    def instance(cls):
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    cls._instance = cls()
        return cls._instance

    def detect(self, frame):
        out = []
        for f in self._app.get(frame):
            pose = getattr(f, "pose", None)
            out.append(DetectedFace(
                bbox=f.bbox.astype(np.float32),
                keypoints=f.kps.astype(np.float32),
                embedding=f.normed_embedding.astype(np.float32),
                detection_score=float(f.det_score),
                pose=np.asarray(pose, dtype=np.float32) if pose is not None else None,
            ))
        return out
