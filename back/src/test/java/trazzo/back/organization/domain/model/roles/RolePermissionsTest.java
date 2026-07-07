package trazzo.back.organization.domain.model.roles;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class RolePermissionsTest {

    @Test
    void create_setsRoleIdPermissionIdAndCreatedAt() {
        var rp = RolePermissions.create("role-1", "perm-1");
        assertThat(rp.getRoleId()).isEqualTo("role-1");
        assertThat(rp.getPermissionId()).isEqualTo("perm-1");
        assertThat(rp.getCreatedAt()).isNotNull();
    }

    @Test
    void restore_setsAllFields() {
        var now = LocalDateTime.now();
        var rp = RolePermissions.restore("role-2", "perm-2", now);
        assertThat(rp.getRoleId()).isEqualTo("role-2");
        assertThat(rp.getPermissionId()).isEqualTo("perm-2");
        assertThat(rp.getCreatedAt()).isEqualTo(now);
    }
}
