package trazzo.back.saasglobal.domain.model.iam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import trazzo.back.saasglobal.domain.exception.UserValidationException;

class UserTest {

    @Test
    void create_setsAllFields() {
        var user = User.create(1, null, "Admin@Test.COM", "999000111", "hashed");

        assertThat(user.getPersonId()).isEqualTo(1);
        assertThat(user.getTenantId()).isNull();
        assertThat(user.getEmail()).isEqualTo("admin@test.com");
        assertThat(user.getPhone()).isEqualTo("999000111");
        assertThat(user.getPassword()).isEqualTo("hashed");
        assertThat(user.getRoles()).isEmpty();
        assertThat(user.getId()).isNotBlank();
        assertThat(user.isActive()).isTrue();
    }

    @Test
    void create_normalizesEmailToLowercase() {
        var user = User.create(1, null, "UPPER@EXAMPLE.COM", null, "hashed");

        assertThat(user.getEmail()).isEqualTo("upper@example.com");
    }

    @Test
    void restore_setsAllFields() {
        LocalDateTime now = LocalDateTime.now();
        var user = User.restore("id-1", 2, "tenant-1", "u@test.com", null,
                "pass", List.of("ADMIN"), List.of("gestion-tenants.crear"), true, now, now, null);

        assertThat(user.getId()).isEqualTo("id-1");
        assertThat(user.getRoles()).containsExactly("ADMIN");
        assertThat(user.getPermissionCodes()).containsExactly("gestion-tenants.crear");
        assertThat(user.isMustChangePassword()).isTrue();
        assertThat(user.isActive()).isTrue();
    }

    @Test
    void isActive_falseWhenDeletedAtSet() {
        LocalDateTime now = LocalDateTime.now();
        var user = User.restore("id-1", 1, null, "u@test.com", null,
                "pass", List.of(), List.of(), false, now, now, now);

        assertThat(user.isActive()).isFalse();
    }

    @Test
    void delete_setsDeletedAt() {
        var user = User.create(1, null, "u@test.com", null, "pass");
        assertThat(user.isActive()).isTrue();

        user.delete();

        assertThat(user.isActive()).isFalse();
        assertThat(user.getDeletedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isEqualTo(user.getDeletedAt());
    }

    @Test
    void delete_throwsWhenAlreadyDeleted() {
        LocalDateTime now = LocalDateTime.now();
        var user = User.restore("id-1", 1, null, "u@test.com", null,
                "pass", List.of(), List.of(), false, now, now, now);

        assertThatThrownBy(user::delete)
                .isInstanceOf(UserValidationException.class)
                .hasMessageContaining("already deleted");
    }

    @Test
    void create_throwsWhenPersonIdNull() {
        assertThatThrownBy(() -> User.create(null, null, "u@test.com", null, "pass"))
                .isInstanceOf(UserValidationException.class)
                .hasMessageContaining("personId");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void create_throwsWhenEmailBlank(String email) {
        assertThatThrownBy(() -> User.create(1, null, email, null, "pass"))
                .isInstanceOf(UserValidationException.class)
                .hasMessageContaining("email");
    }

    @Test
    void create_throwsWhenEmailHasNoAtSign() {
        assertThatThrownBy(() -> User.create(1, null, "notanemail", null, "pass"))
                .isInstanceOf(UserValidationException.class)
                .hasMessageContaining("email");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void create_throwsWhenPasswordBlank(String password) {
        assertThatThrownBy(() -> User.create(1, null, "u@test.com", null, password))
                .isInstanceOf(UserValidationException.class)
                .hasMessageContaining("password");
    }
}
