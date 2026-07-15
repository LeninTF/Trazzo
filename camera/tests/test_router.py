import json
from unittest.mock import AsyncMock, MagicMock

import pytest

from domain.contracts import MsgType
from domain.errors import BiometricError, ErrorCode
from domain.models import MatchResult
from handlers.router import MessageRouter


@pytest.fixture
def router():
    cam = MagicMock()
    cam.is_open.return_value = True

    enrollment = MagicMock()
    enrollment.enroll = AsyncMock(return_value={"personId": "juan", "samples": 3})
    enrollment.delete = MagicMock(return_value=1)

    recognition = MagicMock()
    recognition.verify = AsyncMock(
        return_value=(MatchResult("juan", 0.87), {"liveness": 0.9, "frames": 5}),
    )
    recognition.identify = AsyncMock(
        return_value=(MatchResult("juan", 0.85), {"liveness": 0.9, "frames": 5}),
    )
    return MessageRouter(cam, enrollment, recognition)


async def test_status(router):
    resp = await router.dispatch(json.dumps({"type": MsgType.STATUS}))
    assert resp["type"] == MsgType.STATUS_CHANGED
    assert resp["connected"] is True


async def test_enroll_ok(router):
    resp = await router.dispatch(json.dumps({
        "type": MsgType.ENROLL_START, "personId": "juan", "tenantId": "trazzo",
    }))
    assert resp["type"] == MsgType.ENROLL_RESULT
    assert resp["success"] is True
    assert resp["personId"] == "juan"


async def test_verify_ok(router):
    resp = await router.dispatch(json.dumps({
        "type": MsgType.VERIFY_START, "personId": "juan", "tenantId": "trazzo",
    }))
    assert resp["success"] is True
    assert resp["personId"] == "juan"
    assert resp["score"] == 0.87


async def test_identify_ok(router):
    resp = await router.dispatch(json.dumps({
        "type": MsgType.IDENTIFY_START, "tenantId": "trazzo",
    }))
    assert resp["type"] == MsgType.IDENTIFY_RESULT
    assert resp["success"] is True


async def test_delete_ok(router):
    resp = await router.dispatch(json.dumps({
        "type": MsgType.ENROLL_DELETE, "personId": "juan", "tenantId": "trazzo",
    }))
    assert resp["removed"] == 1


async def test_json_malformado(router):
    resp = await router.dispatch("no soy json {")
    assert resp["type"] == MsgType.ERROR
    assert resp["code"] == ErrorCode.INVALID_JSON.value


async def test_json_no_objeto(router):
    resp = await router.dispatch("[1,2,3]")
    assert resp["code"] == ErrorCode.INVALID_JSON.value


async def test_type_faltante(router):
    resp = await router.dispatch("{}")
    assert resp["code"] == ErrorCode.MISSING_FIELD.value


async def test_tipo_desconocido(router):
    resp = await router.dispatch(json.dumps({"type": "camera.hack"}))
    assert resp["code"] == ErrorCode.UNKNOWN_MESSAGE_TYPE.value


async def test_person_id_invalido_rechazado(router):
    resp = await router.dispatch(json.dumps({
        "type": MsgType.ENROLL_START, "personId": "juan;DROP TABLE users",
    }))
    assert resp["success"] is False
    assert resp["code"] == ErrorCode.VALIDATION_FAILED.value


async def test_person_id_faltante(router):
    resp = await router.dispatch(json.dumps({"type": MsgType.ENROLL_START}))
    assert resp["success"] is False
    assert resp["code"] == ErrorCode.VALIDATION_FAILED.value


async def test_error_del_servicio_mapeado(router):
    router.recognition.verify = AsyncMock(
        side_effect=BiometricError(ErrorCode.NO_MATCH, "no coincide"),
    )
    resp = await router.dispatch(json.dumps({
        "type": MsgType.VERIFY_START, "personId": "juan",
    }))
    assert resp["success"] is False
    assert resp["code"] == ErrorCode.NO_MATCH.value
    assert resp["type"] == MsgType.VERIFY_RESULT


async def test_excepcion_inesperada_no_leakea_stack(router):
    router.recognition.identify = AsyncMock(side_effect=RuntimeError("secreto interno"))
    resp = await router.dispatch(json.dumps({"type": MsgType.IDENTIFY_START}))
    assert resp["code"] == ErrorCode.INTERNAL_ERROR.value
    # el mensaje del error interno NO debe filtrarse
    assert "secreto interno" not in json.dumps(resp)
