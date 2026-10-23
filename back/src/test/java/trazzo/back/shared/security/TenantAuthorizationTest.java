package trazzo.back.shared.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TenantAuthorizationTest {

    private static final UUID MASTER_USER = UUID.fromString("0f0e0d0c-0b0a-4908-8706-050403020100");

    private final TenantUserIdResolver resolver = mock(TenantUserIdResolver.class);
    private final TenantAuthorization authz = new TenantAuthorization(resolver);

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    private static void actingAsTenantUserWith(String... authorities) {
        var user = new AuthenticatedUser(MASTER_USER, "usuario@demo", "", AuthorityUtils.createAuthorityList(authorities), true);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
    }

    private static void actingAsAgent() {
        var agent = new DeviceAgentPrincipal(UUID.randomUUID(), "tenant_demo", 7L, "DVC-001");
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                agent, null, AuthorityUtils.createAuthorityList(DeviceAgentPrincipal.AUTHORITY)));
    }

    @Test
    void any_passesWithOneOfThePermissions() {
        actingAsTenantUserWith("incidencias.ver-propias", "reportes.exportar-pdf");

        assertThat(authz.any("reportes.exportar-excel", "reportes.exportar-pdf")).isTrue();
    }

    @Test
    void any_rejectsWithoutAnyOfThePermissions() {
        actingAsTenantUserWith("incidencias.ver-propias", "incidencias.crear");

        assertThat(authz.any("gestion-trabajadores.crear", "gestion-trabajadores.editar")).isFalse();
    }

    @Test
    void any_alwaysPassesForTheTenantAdministrator() {
        actingAsTenantUserWith("administrador_tenant");

        assertThat(authz.any("gestion-trabajadores.eliminar")).isTrue();
    }

    @Test
    void any_rejectsWithoutAuthentication() {
        assertThat(authz.any("incidencias.ver-propias")).isFalse();
    }

    @Test
    void anUnauthenticatedTokenGrantsNothing() {
        var token = new TestingAuthenticationToken("usuario@demo", null, "administrador_tenant");
        token.setAuthenticated(false);
        SecurityContextHolder.getContext().setAuthentication(token);

        assertThat(authz.any("incidencias.ver-propias")).isFalse();
        assertThat(authz.admin()).isFalse();
    }

    @Test
    void admin_isOnlyTheTenantAdministrator() {
        actingAsTenantUserWith("gestion-trabajadores.crear", "gestion-trabajadores.editar", "gestion-trabajadores.eliminar");
        assertThat(authz.admin()).isFalse();

        actingAsTenantUserWith("administrador_tenant");
        assertThat(authz.admin()).isTrue();
    }

    @Test
    void agent_isOnlyTheReaderAgent() {
        actingAsTenantUserWith("control-asistencia.operador-asistencia", "administrador_tenant");
        assertThat(authz.agent()).isFalse();

        actingAsAgent();
        assertThat(authz.agent()).isTrue();
    }

    @Test
    void currentTenantUserId_resolvesTheTenantUserOfTheSignedInUser() {
        actingAsTenantUserWith("incidencias.ver-propias");
        when(resolver.resolve(MASTER_USER)).thenReturn(Optional.of(42L));

        assertThat(authz.currentTenantUserId()).contains(42L);
    }

    @Test
    void currentTenantUserId_isEmptyForTheAgentAndForAnonymousCallers() {
        actingAsAgent();
        assertThat(authz.currentTenantUserId()).isEmpty();

        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "clave", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        assertThat(authz.currentTenantUserId()).isEmpty();

        SecurityContextHolder.clearContext();
        assertThat(authz.currentTenantUserId()).isEmpty();
        verifyNoInteractions(resolver);
    }

    @Test
    void isSelf_onlyForTheSignedInUsersOwnTenantUser() {
        actingAsTenantUserWith("incidencias.ver-propias");
        when(resolver.resolve(MASTER_USER)).thenReturn(Optional.of(42L));

        assertThat(authz.isSelf(42L)).isTrue();
        assertThat(authz.isSelf(43L)).isFalse();
        assertThat(authz.isSelf(null)).isFalse();
    }

    @Test
    void isSelf_isFalseWhenTheUserHasNoTenantUserInThisTenant() {
        actingAsTenantUserWith("incidencias.ver-propias");
        when(resolver.resolve(MASTER_USER)).thenReturn(Optional.empty());

        assertThat(authz.isSelf(42L)).isFalse();
    }
}
