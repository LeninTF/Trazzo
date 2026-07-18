package trazzo.back.saasglobal.domain.model.iam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import trazzo.back.saasglobal.domain.exception.UserValidationException;

class RoleMasterTest {

    @Test
    void create_setsFields() {
        var role = RoleMaster.create("admin", "Administrador", "Administrador del sistema");

        assertThat(role.getName()).isEqualTo("admin");
        assertThat(role.getDisplayName()).isEqualTo("Administrador");
        assertThat(role.getDescription()).isEqualTo("Administrador del sistema");
        assertThat(role.getId()).isNull();
        assertThat(role.getPermissionCodes()).isEmpty();
    }

    @Test
    void restore_setsFields() {
        var role = RoleMaster.restore(1, "viewer", "Viewer", null, List.of("monitoreo-sistema.logs-sistema"));

        assertThat(role.getId()).isEqualTo(1);
        assertThat(role.getName()).isEqualTo("viewer");
        assertThat(role.getDisplayName()).isEqualTo("Viewer");
        assertThat(role.getDescription()).isNull();
        assertThat(role.getPermissionCodes()).containsExactly("monitoreo-sistema.logs-sistema");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void create_throwsWhenNameBlank(String name) {
        assertThatThrownBy(() -> RoleMaster.create(name, "Display", null))
                .isInstanceOf(UserValidationException.class)
                .hasMessageContaining("name");
    }

    @Test
    void update_changesFields() {
        var role = RoleMaster.create("soporte", "Soporte", "desc original");

        role.update("soporte-v2", "Soporte V2", "desc nueva");

        assertThat(role.getName()).isEqualTo("soporte-v2");
        assertThat(role.getDisplayName()).isEqualTo("Soporte V2");
        assertThat(role.getDescription()).isEqualTo("desc nueva");
    }

    @Test
    void grantPermissions_setsValidCodes() {
        var role = RoleMaster.create("financiero", "Financiero", null);

        role.grantPermissions(List.of("billing-suscripciones.gestionar-pagos", "monitoreo-sistema.dashboard-global"));

        assertThat(role.getPermissionCodes())
                .containsExactlyInAnyOrder("billing-suscripciones.gestionar-pagos", "monitoreo-sistema.dashboard-global");
    }

    @Test
    void grantPermissions_throwsOnUnknownCode() {
        var role = RoleMaster.create("financiero", "Financiero", null);

        assertThatThrownBy(() -> role.grantPermissions(List.of("no-existe.accion")))
                .isInstanceOf(UserValidationException.class)
                .hasMessageContaining("no-existe.accion");
    }
}
