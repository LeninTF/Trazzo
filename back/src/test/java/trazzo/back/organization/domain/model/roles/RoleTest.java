package trazzo.back.organization.domain.model.roles;

import org.junit.jupiter.api.Test;
import trazzo.back.organization.domain.exception.OrgValidationException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class RoleTest {

    @Test
    void create_generatesUuidAndSetsTimestamps() {
        var role = Role.create("Admin", "Administrator role");
        assertThat(role.getId()).isNotBlank();
        assertThat(role.getName()).isEqualTo("Admin");
        assertThat(role.getDescription()).isEqualTo("Administrator role");
        assertThat(role.getCreatedAt()).isNotNull();
        assertThat(role.getUpdatedAt()).isNotNull();
    }

    @Test
    void create_twoInstances_haveDifferentIds() {
        var r1 = Role.create("Admin", null);
        var r2 = Role.create("Manager", null);
        assertThat(r1.getId()).isNotEqualTo(r2.getId());
    }

    @Test
    void create_nullName_throwsValidationException() {
        assertThatThrownBy(() -> Role.create(null, "desc"))
                .isInstanceOf(OrgValidationException.class)
                .hasMessageContaining("name");
    }

    @Test
    void create_blankName_throwsValidationException() {
        assertThatThrownBy(() -> Role.create("  ", "desc"))
                .isInstanceOf(OrgValidationException.class);
    }

    @Test
    void restore_setsAllFields() {
        var now = LocalDateTime.now();
        var role = Role.restore("uuid-1", "Viewer", "Read only", now, now);
        assertThat(role.getId()).isEqualTo("uuid-1");
        assertThat(role.getName()).isEqualTo("Viewer");
        assertThat(role.getDescription()).isEqualTo("Read only");
    }

    @Test
    void update_changesNameAndDescription() {
        var role = Role.create("Old", null);
        role.update("New", "new desc");
        assertThat(role.getName()).isEqualTo("New");
        assertThat(role.getDescription()).isEqualTo("new desc");
        assertThat(role.getUpdatedAt()).isNotNull();
    }

    @Test
    void update_nullName_throwsValidationException() {
        var role = Role.create("Admin", null);
        assertThatThrownBy(() -> role.update(null, "desc"))
                .isInstanceOf(OrgValidationException.class);
    }
}
