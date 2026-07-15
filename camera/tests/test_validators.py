import pytest

from domain.errors import BiometricError, ErrorCode
from domain.validators import validate_person_id, validate_tenant_id, sanitize_log


class TestValidatePersonId:
    @pytest.mark.parametrize("value", [
        "juan", "juan.perez", "user_01", "a", "user-123", "test@domain",
        "A" * 64,  # limite exacto
    ])
    def test_acepta_ids_validos(self, value):
        assert validate_person_id(value) == value

    @pytest.mark.parametrize("value", [
        "juan;DROP TABLE users",
        "juan'--",
        "juan\nADMIN",
        "juan\r\nlogged",
        "juan\x00",
        "juan<script>",
        "juan//../../etc",
        "juan con espacios",
        "",
        "A" * 65,          # 1 sobre el limite
        "A" * 10_000,      # DoS por longitud
    ])
    def test_rechaza_ids_maliciosos(self, value):
        with pytest.raises(BiometricError) as exc:
            validate_person_id(value)
        assert exc.value.code == ErrorCode.VALIDATION_FAILED

    @pytest.mark.parametrize("value", [None, 123, 3.14, [], {}, True])
    def test_rechaza_tipos_no_string(self, value):
        with pytest.raises(BiometricError) as exc:
            validate_person_id(value)
        assert exc.value.code == ErrorCode.VALIDATION_FAILED


class TestValidateTenantId:
    @pytest.mark.parametrize("value", ["default", "trazzo", "org_1", "A" * 32])
    def test_acepta_tenants_validos(self, value):
        assert validate_tenant_id(value) == value

    @pytest.mark.parametrize("value", ["", "trazzo.dot", "trazzo@", "trazzo/", "A" * 33])
    def test_rechaza_tenants_invalidos(self, value):
        with pytest.raises(BiometricError):
            validate_tenant_id(value)


class TestSanitizeLog:
    def test_reemplaza_control_chars(self):
        assert "\n" not in sanitize_log("juan\ninject")
        assert "\r" not in sanitize_log("juan\rinject")
        assert "\x00" not in sanitize_log("juan\x00null")

    def test_reemplaza_con_placeholder(self):
        assert sanitize_log("a\nb") == "a?b"

    def test_trunca_muy_largo(self):
        s = sanitize_log("A" * 500)
        assert len(s) <= 210
        assert s.endswith("...")

    def test_no_string_devuelve_repr(self):
        assert sanitize_log(123) == "123"
        assert sanitize_log(None) == "None"

    def test_string_normal_intacto(self):
        assert sanitize_log("hola mundo") == "hola mundo"
