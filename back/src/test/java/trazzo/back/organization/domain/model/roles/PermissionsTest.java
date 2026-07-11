package trazzo.back.organization.domain.model.roles;

import org.junit.jupiter.api.Test;
import trazzo.back.organization.domain.exception.OrgValidationException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class PermissionsTest {

    @Test
    void create_generatesUuidAndSetsFields() {
        var perm = Permissions.create("READ_USERS", "Can read users", "USER_MGMT");
        assertThat(perm.getId()).isNotBlank();
        assertThat(perm.getName()).isEqualTo("READ_USERS");
        assertThat(perm.getDescription()).isEqualTo("Can read users");
        assertThat(perm.getMasterFeaturesCode()).isEqualTo("USER_MGMT");
        assertThat(perm.getCreatedAt()).isNotNull();
        assertThat(perm.getUpdatedAt()).isNotNull();
    }

    @Test
    void create_twoInstances_haveDifferentIds() {
        var p1 = Permissions.create("READ", null, null);
        var p2 = Permissions.create("WRITE", null, null);
        assertThat(p1.getId()).isNotEqualTo(p2.getId());
    }

    @Test
    void create_nullName_throwsValidationException() {
        assertThatThrownBy(() -> Permissions.create(null, "desc", "code"))
                .isInstanceOf(OrgValidationException.class)
                .hasMessageContaining("name");
    }

    @Test
    void create_blankName_throwsValidationException() {
        assertThatThrownBy(() -> Permissions.create("  ", "desc", "code"))
                .isInstanceOf(OrgValidationException.class);
    }

    @Test
    void restore_setsAllFields() {
        var now = LocalDateTime.now();
        var perm = Permissions.restore("uuid-p1", "WRITE", "Write access", "FEAT_01", now, now);
        assertThat(perm.getId()).isEqualTo("uuid-p1");
        assertThat(perm.getName()).isEqualTo("WRITE");
        assertThat(perm.getMasterFeaturesCode()).isEqualTo("FEAT_01");
    }

    @Test
    void update_changesAllFields() {
        var perm = Permissions.create("READ", "old desc", "OLD_CODE");
        perm.update("WRITE", "new desc", "NEW_CODE");
        assertThat(perm.getName()).isEqualTo("WRITE");
        assertThat(perm.getDescription()).isEqualTo("new desc");
        assertThat(perm.getMasterFeaturesCode()).isEqualTo("NEW_CODE");
        assertThat(perm.getUpdatedAt()).isNotNull();
    }

    @Test
    void update_nullName_throwsValidationException() {
        var perm = Permissions.create("READ", null, null);
        assertThatThrownBy(() -> perm.update(null, "desc", "code"))
                .isInstanceOf(OrgValidationException.class);
    }
}
