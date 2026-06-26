package trazzo.back.saasglobal.domain.model.iam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import trazzo.back.saasglobal.domain.exception.UserValidationException;

class RoleMasterTest {

    @Test
    void create_setsFields() {
        var role = RoleMaster.create("ADMIN", "Administrador del sistema");

        assertThat(role.getName()).isEqualTo("ADMIN");
        assertThat(role.getDescription()).isEqualTo("Administrador del sistema");
        assertThat(role.getId()).isNull();
    }

    @Test
    void restore_setsFields() {
        var role = RoleMaster.restore(1, "VIEWER", null);

        assertThat(role.getId()).isEqualTo(1);
        assertThat(role.getName()).isEqualTo("VIEWER");
        assertThat(role.getDescription()).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    void create_throwsWhenNameBlank(String name) {
        assertThatThrownBy(() -> RoleMaster.create(name, null))
                .isInstanceOf(UserValidationException.class)
                .hasMessageContaining("name");
    }
}
