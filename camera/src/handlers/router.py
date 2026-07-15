import json
import logging
from typing import Any

from domain.errors import BiometricError, ErrorCode
from domain.contracts import MsgType, error_response
from domain.validators import validate_person_id, validate_tenant_id, sanitize_log

log = logging.getLogger(__name__)


_RESULT_TYPES = {
    MsgType.ENROLL_START: MsgType.ENROLL_RESULT,
    MsgType.ENROLL_DELETE: MsgType.ENROLL_DELETE_RESULT,
    MsgType.VERIFY_START: MsgType.VERIFY_RESULT,
    MsgType.IDENTIFY_START: MsgType.IDENTIFY_RESULT,
}


class MessageRouter:
    def __init__(self, camera, enrollment, recognition) -> None:
        self.camera = camera
        self.enrollment = enrollment
        self.recognition = recognition

    async def dispatch(self, raw: str) -> dict:
        try:
            msg = json.loads(raw)
        except json.JSONDecodeError:
            return error_response(ErrorCode.INVALID_JSON.value)

        if not isinstance(msg, dict):
            return error_response(ErrorCode.INVALID_JSON.value, "expected JSON object")

        msg_type = msg.get("type")
        if not isinstance(msg_type, str):
            return error_response(ErrorCode.MISSING_FIELD.value, "type")

        try:
            return await self._route(msg, msg_type)
        except BiometricError as e:
            return {
                "type": _RESULT_TYPES.get(msg_type, MsgType.ERROR),
                "success": False,
                "code": e.code.value,
                "message": e.message,
            }
        except Exception:
            # Nunca exponer stack traces al cliente; log detallado internamente
            log.exception("error inesperado en tipo=%s", sanitize_log(msg_type))
            return error_response(ErrorCode.INTERNAL_ERROR.value, "internal error")

    async def _route(self, msg: dict[str, Any], msg_type: str) -> dict:
        tenant = validate_tenant_id(msg.get("tenantId", "default"))

        if msg_type == MsgType.STATUS:
            return {"type": MsgType.STATUS_CHANGED, "connected": self.camera.is_open()}

        if msg_type == MsgType.ENROLL_START:
            person = validate_person_id(msg.get("personId"))
            log.info("enroll requested tenant=%s person=%s",
                     sanitize_log(tenant), sanitize_log(person))
            payload = await self.enrollment.enroll(person, tenant)
            return {"type": MsgType.ENROLL_RESULT, "success": True, **payload}

        if msg_type == MsgType.ENROLL_DELETE:
            person = validate_person_id(msg.get("personId"))
            removed = self.enrollment.delete(person, tenant)
            return {"type": MsgType.ENROLL_DELETE_RESULT, "success": True, "removed": removed}

        if msg_type == MsgType.VERIFY_START:
            person = validate_person_id(msg.get("personId"))
            match, meta = await self.recognition.verify(person, tenant)
            return {
                "type": MsgType.VERIFY_RESULT,
                "success": True,
                "personId": match.person_id,
                "score": match.score,
                **meta,
            }

        if msg_type == MsgType.IDENTIFY_START:
            match, meta = await self.recognition.identify(tenant)
            return {
                "type": MsgType.IDENTIFY_RESULT,
                "success": True,
                "personId": match.person_id,
                "score": match.score,
                **meta,
            }

        return error_response(ErrorCode.UNKNOWN_MESSAGE_TYPE.value, sanitize_log(msg_type))
