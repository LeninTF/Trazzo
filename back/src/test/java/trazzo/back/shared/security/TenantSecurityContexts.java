package trazzo.back.shared.security;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/** Usuarios de tenant autenticados para los slice tests, como los deja JwtAuthFilter. */
public final class TenantSecurityContexts {

    public static final UUID MASTER_USER_ID = UUID.fromString("6f1d2c3b-4a59-4e68-9a7b-8c9d0e1f2a3b");

    /** Permisos del rol "usuario" que siembra V3_1 (el rol mínimo del tenant). */
    public static final String[] PLAIN_USER_PERMISSIONS = {"incidencias.ver-propias", "incidencias.crear",
            "asistencia-docente.ver-historial", "perfil.ver-actualizar-datos"};

    private TenantSecurityContexts() {
    }

    public static void actingAsTenantUserWith(String... authorities) {
        var user = new AuthenticatedUser(MASTER_USER_ID, "usuario@demo.trazzo.pe", "",
                AuthorityUtils.createAuthorityList(authorities), true);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
    }

    public static void actingAsTenantAdmin() {
        actingAsTenantUserWith(TenantAuthorization.TENANT_ADMIN);
    }

    public static void actingAsPlainUser() {
        actingAsTenantUserWith(PLAIN_USER_PERMISSIONS);
    }

    /** Lo registra {@link TenantSecuritySlice}: administrador antes de cada test y contexto limpio después. */
    public static final class TenantAdminByDefault implements BeforeEachCallback, AfterEachCallback {

        @Override
        public void beforeEach(ExtensionContext context) {
            actingAsTenantAdmin();
        }

        @Override
        public void afterEach(ExtensionContext context) {
            SecurityContextHolder.clearContext();
        }
    }
}
