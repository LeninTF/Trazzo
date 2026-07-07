package trazzo.back.organization.domain.model.roles;

import org.junit.jupiter.api.Test;
import trazzo.back.organization.domain.exception.OrgValidationException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class TenantUserRoleTest {

    @Test
    void create_setsAllFields() {
        var tur = TenantUserRole.create(10L, "role-uuid", 5L);
        assertThat(tur.getId()).isNull();
        assertThat(tur.getTenantUserId()).isEqualTo(10L);
        assertThat(tur.getRoleId()).isEqualTo("role-uuid");
        assertThat(tur.getDepartmentId()).isEqualTo(5L);
        assertThat(tur.getCreatedAt()).isNotNull();
    }

    @Test
    void create_withNullDepartmentId_isAllowed() {
        var tur = TenantUserRole.create(1L, "role-uuid", null);
        assertThat(tur.getDepartmentId()).isNull();
    }

    @Test
    void create_nullTenantUserId_throwsValidationException() {
        assertThatThrownBy(() -> TenantUserRole.create(null, "role-uuid", 1L))
                .isInstanceOf(OrgValidationException.class)
                .hasMessageContaining("tenantUserId");
    }

    @Test
    void create_nullRoleId_throwsValidationException() {
        assertThatThrownBy(() -> TenantUserRole.create(1L, null, 1L))
                .isInstanceOf(OrgValidationException.class)
                .hasMessageContaining("roleId");
    }

    @Test
    void create_blankRoleId_throwsValidationException() {
        assertThatThrownBy(() -> TenantUserRole.create(1L, "  ", 1L))
                .isInstanceOf(OrgValidationException.class);
    }

    @Test
    void restore_setsAllFields() {
        var now = LocalDateTime.now();
        var tur = TenantUserRole.restore(99L, 7L, "uuid-role", 3L, now);
        assertThat(tur.getId()).isEqualTo(99L);
        assertThat(tur.getTenantUserId()).isEqualTo(7L);
        assertThat(tur.getRoleId()).isEqualTo("uuid-role");
        assertThat(tur.getDepartmentId()).isEqualTo(3L);
        assertThat(tur.getCreatedAt()).isEqualTo(now);
    }
}
