import os
from dataclasses import dataclass
from typing import Final

# limitar threads antes de importar onnxruntime.
os.environ.setdefault("OMP_NUM_THREADS", "2")
os.environ.setdefault("OMP_WAIT_POLICY", "PASSIVE")
os.environ.setdefault("MKL_NUM_THREADS", "2")

_PROGRAMDATA: Final[str] = os.environ.get("PROGRAMDATA", r"C:\ProgramData")
_BASE_DIR: Final[str] = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


@dataclass(frozen=True)
class Settings:
    websocket_host: str
    websocket_port: int
    max_message_bytes: int
    max_concurrent_clients: int
    rate_limit_capture_s: float

    camera_index: int
    camera_backend: str
    camera_width: int
    camera_height: int
    opencv_threads: int
    camera_warmup_frames: int
    camera_open_timeout_s: float
    camera_read_timeout_s: float

    models_dir: str
    model_pack: str
    ort_intra_threads: int

    detection_threshold: float
    recognition_threshold: float
    recognition_margin: float
    min_face_size: int

    quality_min_detection_score: float
    quality_max_yaw: float
    quality_min_pitch: float
    quality_max_pitch: float
    quality_min_sharpness: float
    identity_min_similarity: float

    enrollment_samples: int
    capture_max_seconds: float
    capture_min_valid_frames: int

    liveness_min_score: float
    anti_spoof_min_real: float
    anti_spoof_required: bool

    db_path: str
    max_gallery_per_tenant: int


def _int(key: str, default: int, minimum: int | None = None, maximum: int | None = None) -> int:
    raw = os.getenv(key)
    try:
        value = int(raw) if raw is not None else default
    except (TypeError, ValueError):
        value = default
    if minimum is not None and value < minimum:
        value = minimum
    if maximum is not None and value > maximum:
        value = maximum
    return value


def _float(key: str, default: float, minimum: float | None = None, maximum: float | None = None) -> float:
    raw = os.getenv(key)
    try:
        value = float(raw) if raw is not None else default
    except (TypeError, ValueError):
        value = default
    if minimum is not None and value < minimum:
        value = minimum
    if maximum is not None and value > maximum:
        value = maximum
    return value


def _str(key: str, default: str) -> str:
    return os.getenv(key, default)


def _bool(key: str, default: bool) -> bool:
    raw = os.getenv(key)
    if raw is None:
        return default
    return raw.strip().lower() in ("1", "true", "yes", "on")


def _resolve_safe_path(user_path: str, allowed_root: str) -> str:
    """Resuelve absolutamente y verifica que quede dentro de allowed_root.
    Previene path traversal via env vars maliciosos (DB_PATH=../../../etc/passwd).
    """
    absolute = os.path.abspath(user_path)
    root = os.path.abspath(allowed_root)
    if not absolute.startswith(root + os.sep) and absolute != root:
        raise ValueError(f"path fuera del root permitido: {absolute} not in {root}")
    return absolute


def _load() -> Settings:
    db_path = _str("DB_PATH", os.path.join(_PROGRAMDATA, "TrazzoAgent", "faces.db"))
    models_dir = _str("MODELS_DIR", os.path.join(_BASE_DIR, "models"))

    # verificacion basica: rutas absolutas y sin componentes ..
    for path in (db_path, models_dir):
        if ".." in path.split(os.sep):
            raise ValueError(f"ruta contiene '..': {path}")

    return Settings(
        websocket_host=_str("WEBSOCKET_HOST", "localhost"),
        websocket_port=_int("WEBSOCKET_PORT", 9002, 1, 65535),
        max_message_bytes=_int("MAX_MESSAGE_BYTES", 4096, 128, 65536),
        max_concurrent_clients=_int("MAX_CONCURRENT_CLIENTS", 10, 1, 200),
        rate_limit_capture_s=_float("RATE_LIMIT_CAPTURE_S", 1.0, 0.0, 60.0),
        camera_index=_int("CAMERA_INDEX", 1, 0, 10),
        camera_backend=_str("CAMERA_BACKEND", "dshow"),
        # 0 = usar la máxima resolución que soporte la cámara (recomendado para precisión).
        # Fijar ambos solo si se necesita forzar una resolución concreta.
        camera_width=_int("CAMERA_WIDTH", 0, 0, 7680),
        camera_height=_int("CAMERA_HEIGHT", 0, 0, 4320),
        # hilos de OpenCV; con más de 1-2 compiten con ONNX Runtime y frenan la inferencia
        opencv_threads=_int("OPENCV_THREADS", 1, 1, 64),
        camera_warmup_frames=_int("CAMERA_WARMUP_FRAMES", 15, 0, 200),
        camera_open_timeout_s=_float("CAMERA_OPEN_TIMEOUT_S", 3.0, 0.5, 30.0),
        camera_read_timeout_s=_float("CAMERA_READ_TIMEOUT_S", 2.0, 0.5, 10.0),
        models_dir=os.path.abspath(models_dir),
        # trazzo_r50 = detector SCRFD liviano + ArcFace ResNet-50 (scripts/download_models.py).
        # Si no está instalado, face_engine usa buffalo_sc (incluido en el repo) y lo registra.
        model_pack=_str("MODEL_PACK", "trazzo_r50"),
        # hilos por operación de ONNX Runtime (ver infrastructure/face_engine._session_options).
        # 2 hilos: ~120 ms por reconocimiento usando ~2 núcleos; más hilos casi no bajan latencia.
        ort_intra_threads=_int("ORT_INTRA_THREADS", 2, 1, 64),
        detection_threshold=_float("DETECTION_THRESHOLD", 0.5, 0.1, 0.99),
        # Calibrado con LFW para trazzo_r50 (scripts/calibrate_recognition.py, 2026-09-16):
        # 656k pares de personas distintas, maximo 0.346 -> 0.40 deja margen y no produjo ningun
        # falso match; rechaza 1.5% de fotos sueltas de la misma persona (en vivo se promedian
        # varios frames). El 0.55 anterior rechazaba el 10%. Recalibrar si cambia MODEL_PACK.
        recognition_threshold=_float("RECOGNITION_THRESHOLD", 0.40, 0.1, 0.99),
        # Diferencia mínima entre el mejor y el segundo mejor candidato en identificación 1:N.
        # Evita elegir a la persona equivocada cuando dos enroladas puntúan casi igual.
        recognition_margin=_float("RECOGNITION_MARGIN", 0.05, 0.0, 1.0),
        min_face_size=_int("MIN_FACE_SIZE", 80, 20, 1000),
        # Calidad mínima de cada frame usado para enrolar/reconocer (ver services/face_quality.py)
        quality_min_detection_score=_float("QUALITY_MIN_DETECTION_SCORE", 0.6, 0.0, 1.0),
        quality_max_yaw=_float("QUALITY_MAX_YAW", 0.15, 0.01, 1.0),
        quality_min_pitch=_float("QUALITY_MIN_PITCH", 0.25, 0.0, 1.0),
        quality_max_pitch=_float("QUALITY_MAX_PITCH", 0.80, 0.0, 1.5),
        # Calibrado con la cámara real a 1080p: frames normales dieron nitidez mínima 35-78
        # (mediana 60-124) y frames desenfocados (gauss sigma=3) un máximo de 14-30.
        quality_min_sharpness=_float("QUALITY_MIN_SHARPNESS", 30.0, 0.0, 10000.0),
        # Similitud mínima entre frames/muestras de UNA captura para considerarlos la misma
        # persona. Medido con buffalo_sc: misma persona frame a frame min 0.61-0.85; personas
        # distintas quedan típicamente < 0.3.
        identity_min_similarity=_float("IDENTITY_MIN_SIMILARITY", 0.5, 0.0, 1.0),
        enrollment_samples=_int("ENROLLMENT_SAMPLES", 1, 1, 10),
        capture_max_seconds=_float("CAPTURE_MAX_SECONDS", 8.0, 1.0, 60.0),
        capture_min_valid_frames=_int("CAPTURE_MIN_VALID_FRAMES", 10, 1, 30),
        liveness_min_score=_float("LIVENESS_MIN_SCORE", 0.40, 0.0, 1.0),
        # Umbral sobre el promedio de los dos modelos anti-spoof, por frame. Con una persona real
        # (RGB corregido) el p10 medido fue 0.60; fotos/pantallas quedan cerca de 0.
        anti_spoof_min_real=_float("ANTI_SPOOF_MIN_REAL", 0.6, 0.0, 1.0),
        # Si es True y los modelos del CNN no están cargados, la captura se rechaza
        # (fail-closed) en vez de degradar abierto. Recomendado en producción.
        anti_spoof_required=_bool("ANTI_SPOOF_REQUIRED", False),
        db_path=os.path.abspath(db_path),
        max_gallery_per_tenant=_int("MAX_GALLERY_PER_TENANT", 5000, 1, 1_000_000),
    )


SETTINGS: Final[Settings] = _load()
