"""Validaciones de entrada: whitelist regex + normalizacion.

Defense-in-depth: aunque la DB use parametros, validamos aca para:
  - fallar rapido con mensaje claro
  - impedir person_ids absurdos (miles de caracteres, control chars)
  - normalizar antes de indexar en la DB
  - prevenir log injection (newlines/control chars en logs)
"""
import re
from typing import Final

from domain.errors import BiometricError, ErrorCode

_PERSON_ID_MAX: Final[int] = 64
_TENANT_ID_MAX: Final[int] = 32
_LOG_MAX: Final[int] = 200

_PERSON_ID_RE: Final = re.compile(r"^[A-Za-z0-9_\-\.@]{1," + str(_PERSON_ID_MAX) + r"}$")
_TENANT_ID_RE: Final = re.compile(r"^[A-Za-z0-9_\-]{1," + str(_TENANT_ID_MAX) + r"}$")
_CTRL_CHARS_RE: Final = re.compile(r"[\x00-\x1f\x7f]")


def validate_person_id(value: object) -> str:
    if not isinstance(value, str) or not _PERSON_ID_RE.match(value):
        raise BiometricError(
            ErrorCode.VALIDATION_FAILED,
            f"personId invalido (alfanumerico, _-.@, max {_PERSON_ID_MAX} chars)",
        )
    return value


def validate_tenant_id(value: object) -> str:
    if not isinstance(value, str) or not _TENANT_ID_RE.match(value):
        raise BiometricError(
            ErrorCode.VALIDATION_FAILED,
            f"tenantId invalido (alfanumerico, _-, max {_TENANT_ID_MAX} chars)",
        )
    return value


def sanitize_log(value: object) -> str:
    """Sanea antes de loggear entrada de usuario.

    Previene log injection: alguien que meta '\\n[INFO] admin logged in'
    en un personId no puede forjar entradas de log.
    """
    text = value if isinstance(value, str) else repr(value)
    text = _CTRL_CHARS_RE.sub("?", text)
    if len(text) > _LOG_MAX:
        text = text[:_LOG_MAX] + "..."
    return text
