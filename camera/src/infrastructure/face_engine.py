import logging
import os
import threading
import numpy as np
import onnxruntime as ort

# NOTA: config debe importarse primero — setea OMP_NUM_THREADS antes de onnxruntime
from config import SETTINGS
from insightface.app import FaceAnalysis

from domain.models import DetectedFace

log = logging.getLogger(__name__)

_EMPTY_EMBEDDING = np.zeros(0, dtype=np.float32)


def _session_options() -> ort.SessionOptions:
    """Presupuesto de CPU acotado y latencia estable para ONNX Runtime.

    Por defecto ORT crea, POR SESIÓN, un pool con tantos hilos como núcleos y los deja en espera
    activa (spinning): con detector + reconocedor + anti-spoof el proceso llegó a ocupar ~20
    núcleos. Pocos hilos y sin spinning es la práctica estándar en equipos compartidos, y en CPUs
    híbridas (núcleos P + E) además evita que cada operación espere al núcleo más lento.
    """
    so = ort.SessionOptions()
    so.graph_optimization_level = ort.GraphOptimizationLevel.ORT_ENABLE_ALL
    so.execution_mode = ort.ExecutionMode.ORT_SEQUENTIAL
    so.intra_op_num_threads = SETTINGS.ort_intra_threads
    so.inter_op_num_threads = 1
    so.add_session_config_entry("session.intra_op.allow_spinning", "0")
    so.add_session_config_entry("session.inter_op.allow_spinning", "0")
    return so


_FALLBACK_PACK = "buffalo_sc"  # incluido en el repositorio


def _pack_available(name: str) -> bool:
    pack_dir = os.path.join(SETTINGS.models_dir, "models", name)
    return os.path.isdir(pack_dir) and any(f.endswith(".onnx") for f in os.listdir(pack_dir))


def _ensure_model_pack() -> None:
    """Si el pack configurado no está instalado, usa el pack del repositorio en vez de caer.

    Se actualiza SETTINGS.model_pack porque la base etiqueta cada plantilla con el modelo que la
    generó: comparar embeddings de modelos distintos produciría coincidencias sin sentido.
    """
    if _pack_available(SETTINGS.model_pack) or SETTINGS.model_pack == _FALLBACK_PACK:
        return
    log.critical("pack '%s' no instalado (ejecuta scripts/download_models.py); usando '%s'. "
                 "Las plantillas registradas con '%s' no se compararan hasta instalarlo.",
                 SETTINGS.model_pack, _FALLBACK_PACK, SETTINGS.model_pack)
    object.__setattr__(SETTINGS, "model_pack", _FALLBACK_PACK)


class FaceEngine:
    _instance = None
    _lock = threading.Lock()

    def __init__(self):
        _ensure_model_pack()
        self._app = FaceAnalysis(
            name=SETTINGS.model_pack,
            root=SETTINGS.models_dir,
            providers=["CPUExecutionProvider"],
            # Solo lo que usa el sistema. Packs como buffalo_l incluyen además landmarks 3D,
            # landmarks 106 y edad/género, que se ejecutarían en CADA frame sin usarse.
            allowed_modules=["detection", "recognition"],
        )
        self._apply_session_options()
        self._app.prepare(ctx_id=0, det_thresh=SETTINGS.detection_threshold, det_size=(640, 640))
        # det_model también funciona sin recognition; se guarda la referencia para detect_only
        self._detector = self._app.det_model
        log.info("engine listo (pack=%s, ort_threads=%d por sesion, sin spinning)",
                 SETTINGS.model_pack, SETTINGS.ort_intra_threads)

    def _apply_session_options(self) -> None:
        # insightface.model_zoo.get_model solo reenvía `providers`: IGNORA `sess_options`.
        # Se recrea cada sesión con opciones explícitas. Mismo archivo ONNX => mismos nombres de
        # entrada/salida, que los wrappers (SCRFD, ArcFaceONNX) ya leyeron en su __init__.
        options = _session_options()
        for task, model in self._app.models.items():
            model.session = ort.InferenceSession(
                model.model_file, sess_options=options, providers=["CPUExecutionProvider"],
            )
            log.debug("sesion %s recreada con hilos acotados", task)

    @classmethod
    def instance(cls):
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    cls._instance = cls()
        return cls._instance

    def detect(self, frame):
        """Detección + embedding de reconocimiento (costoso). Solo para enrolar/reconocer."""
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

    def detect_only(self, frame):
        """Solo detección (sin embedding): barato, pensado para previews o para saber si hay un
        rostro antes de lanzar el pipeline completo. `embedding` va vacío."""
        bboxes, kpss = self._detector.detect(frame, max_num=0, metric="default")
        out = []
        for i in range(bboxes.shape[0]):
            out.append(DetectedFace(
                bbox=bboxes[i, :4].astype(np.float32),
                keypoints=(kpss[i] if kpss is not None else np.zeros((5, 2))).astype(np.float32),
                embedding=_EMPTY_EMBEDDING,
                detection_score=float(bboxes[i, 4]),
            ))
        return out
