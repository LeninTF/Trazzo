from enum import Enum


class ErrorCode(str, Enum):
    INVALID_JSON = "INVALID_JSON"
    MISSING_FIELD = "MISSING_FIELD"
    UNKNOWN_MESSAGE_TYPE = "UNKNOWN_MESSAGE_TYPE"
    VALIDATION_FAILED = "VALIDATION_FAILED"
    MESSAGE_TOO_LARGE = "MESSAGE_TOO_LARGE"
    RATE_LIMITED = "RATE_LIMITED"

    CAMERA_UNAVAILABLE = "CAMERA_UNAVAILABLE"
    CAMERA_FRAME_INVALID = "CAMERA_FRAME_INVALID"
    CAMERA_TIMEOUT = "CAMERA_TIMEOUT"

    NO_FACE_DETECTED = "NO_FACE_DETECTED"
    MULTIPLE_FACES = "MULTIPLE_FACES"
    FACE_TOO_SMALL = "FACE_TOO_SMALL"
    FACE_LOW_QUALITY = "FACE_LOW_QUALITY"

    LIVENESS_FAILED = "LIVENESS_FAILED"
    SPOOF_DETECTED = "SPOOF_DETECTED"

    NO_ENROLLED_FACES = "NO_ENROLLED_FACES"
    PERSON_NOT_ENROLLED = "PERSON_NOT_ENROLLED"
    NO_MATCH = "NO_MATCH"
    GALLERY_FULL = "GALLERY_FULL"

    INTERNAL_ERROR = "INTERNAL_ERROR"


class BiometricError(Exception):
    __slots__ = ("code", "message")

    def __init__(self, code: ErrorCode, message: str | None = None) -> None:
        super().__init__(message or code.value)
        self.code = code
        self.message = message or code.value
