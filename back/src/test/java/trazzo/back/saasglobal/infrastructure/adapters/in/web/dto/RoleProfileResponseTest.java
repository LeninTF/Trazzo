package trazzo.back.saasglobal.infrastructure.adapters.in.web.dto;

import org.junit.jupiter.api.Test;
import trazzo.back.saasglobal.domain.model.iam.RoleMaster;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RoleProfileResponseTest {

    @Test
    void fromRoleName_createsResponseWithEmptyPermissions() {
        var result = RoleProfileResponse.fromRoleName("admin_trazzo");

        assertThat(result.id()).isEqualTo(0);
        assertThat(result.name()).isEqualTo("admin_trazzo");
        assertThat(result.permissions()).isEmpty();
    }

    @Test
    void fromRoleName_handlesNullName() {
        var result = RoleProfileResponse.fromRoleName(null);

        assertThat(result.id()).isEqualTo(0);
        assertThat(result.name()).isNull();
        assertThat(result.permissions()).isEmpty();
    }

    @Test
    void from_createsResponseFromRoleMaster() {
        var role = RoleMaster.restore(1, "soporte", "Soporte", "desc",
                List.of("monitoreo-sistema.dashboard-global"));

        var result = RoleProfileResponse.from(role);

        assertThat(result.id()).isEqualTo(1);
        assertThat(result.name()).isEqualTo("soporte");
        assertThat(result.permissions()).containsEntry("monitoreo-sistema.dashboard-global", true);
    }

    @Test
    void from_mapsMultiplePermissions() {
        var role = RoleMaster.restore(2, "admin", "Admin", "desc",
                List.of("perm1", "perm2", "perm3"));

        var result = RoleProfileResponse.from(role);

        assertThat(result.permissions()).hasSize(3);
        assertThat(result.permissions()).containsEntry("perm1", true);
        assertThat(result.permissions()).containsEntry("perm2", true);
        assertThat(result.permissions()).containsEntry("perm3", true);
    }

    @Test
    void from_handlesEmptyPermissions() {
        var role = RoleMaster.restore(3, "empty", "Empty", "desc", List.of());

        var result = RoleProfileResponse.from(role);

        assertThat(result.permissions()).isEmpty();
    }
}
