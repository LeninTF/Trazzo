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
    camera_warmup_frames: int
    camera_open_timeout_s: float
    camera_read_timeout_s: float

    models_dir: str
    model_pack: str

    detection_threshold: float
    recognition_threshold: float
    min_face_size: int

    enrollment_samples: int
    capture_max_seconds: float
    capture_min_valid_frames: int

    liveness_min_score: float
    anti_spoof_min_real: float

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
        camera_warmup_frames=_int("CAMERA_WARMUP_FRAMES", 15, 0, 200),
        camera_open_timeout_s=_float("CAMERA_OPEN_TIMEOUT_S", 3.0, 0.5, 30.0),
        camera_read_timeout_s=_float("CAMERA_READ_TIMEOUT_S", 2.0, 0.5, 10.0),
        models_dir=os.path.abspath(models_dir),
        model_pack=_str("MODEL_PACK", "buffalo_sc"),
        detection_threshold=_float("DETECTION_THRESHOLD", 0.5, 0.1, 0.99),
        recognition_threshold=_float("RECOGNITION_THRESHOLD", 0.55, 0.1, 0.99),
        min_face_size=_int("MIN_FACE_SIZE", 80, 20, 1000),
        enrollment_samples=_int("ENROLLMENT_SAMPLES", 1, 1, 10),
        capture_max_seconds=_float("CAPTURE_MAX_SECONDS", 8.0, 1.0, 60.0),
        capture_min_valid_frames=_int("CAPTURE_MIN_VALID_FRAMES", 10, 1, 30),
        liveness_min_score=_float("LIVENESS_MIN_SCORE", 0.40, 0.0, 1.0),
        anti_spoof_min_real=_float("ANTI_SPOOF_MIN_REAL", 0.85, 0.0, 1.0),
        db_path=os.path.abspath(db_path),
        max_gallery_per_tenant=_int("MAX_GALLERY_PER_TENANT", 5000, 1, 1_000_000),
    )


SETTINGS: Final[Settings] = _load()
