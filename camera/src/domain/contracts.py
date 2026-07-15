class MsgType:
    ENROLL_START = "camera.enroll.start"
    ENROLL_DELETE = "camera.enroll.delete"
    VERIFY_START = "camera.verify.start"
    IDENTIFY_START = "camera.identify.start"
    STATUS = "camera.status"

    ENROLL_RESULT = "camera.enroll.result"
    ENROLL_DELETE_RESULT = "camera.enroll.delete.result"
    VERIFY_RESULT = "camera.verify.result"
    IDENTIFY_RESULT = "camera.identify.result"
    STATUS_CHANGED = "camera.status.changed"
    ERROR = "camera.error"


def error_response(code, message=None):
    payload = {"type": MsgType.ERROR, "code": code}
    if message:
        payload["message"] = message
    return payload
