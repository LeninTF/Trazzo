package trazzo.back.saasglobal.domain.model.iam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import trazzo.back.saasglobal.domain.exception.UserValidationException;

class MetodoRecuperacionTest {

    @Test
    void create_setsAllFields() {
        var m = MetodoRecuperacion.create("user-id-1", MetodoRecuperacion.Type.EMAIL, "test@example.com");

        assertThat(m.getUsersId()).isEqualTo("user-id-1");
        assertThat(m.getMethodType()).isEqualTo(MetodoRecuperacion.Type.EMAIL);
        assertThat(m.getValue()).isEqualTo("test@example.com");
        assertThat(m.getId()).isNull();
        assertThat(m.getDeletedAt()).isNull();
        assertThat(m.getCreatedAt()).isNotNull();
    }

    @Test
    void restore_setsAllFields() {
        LocalDateTime now = LocalDateTime.now();
        var m = MetodoRecuperacion.restore(1, "user-id-2", MetodoRecuperacion.Type.PHONE,
                "+51999000111", now, now, null);

        assertThat(m.getId()).isEqualTo(1);
        assertThat(m.getMethodType()).isEqualTo(MetodoRecuperacion.Type.PHONE);
        assertThat(m.getValue()).isEqualTo("+51999000111");
    }

    @Test
    void create_throwsWhenUserIdBlank() {
        assertThatThrownBy(() ->
                MetodoRecuperacion.create("", MetodoRecuperacion.Type.EMAIL, "v@test.com"))
                .isInstanceOf(UserValidationException.class)
                .hasMessageContaining("usersId");
    }

    @Test
    void create_throwsWhenMethodTypeNull() {
        assertThatThrownBy(() ->
                MetodoRecuperacion.create("user-id", null, "value"))
                .isInstanceOf(UserValidationException.class)
                .hasMessageContaining("methodType");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void create_throwsWhenValueBlank(String value) {
        assertThatThrownBy(() ->
                MetodoRecuperacion.create("user-id", MetodoRecuperacion.Type.EMAIL, value))
                .isInstanceOf(UserValidationException.class)
                .hasMessageContaining("value");
    }
}
