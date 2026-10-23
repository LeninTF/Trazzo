package trazzo.back.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import java.io.IOException;
import java.sql.DatabaseMetaData;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import trazzo.back.shared.tenancy.TenantContext;

class TenantContextGuardTest {

    private final FilterChain chain = mock(FilterChain.class);

    // Lo que deja JwtAuthFilter para un admin SaaS: autenticado y sin tenant (TenantContext "public").
    @BeforeEach
    void aSaasAdminIsSignedIn() {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                "admin@trazzo.pe", null, AuthorityUtils.createAuthorityList("ROLE_SAAS_ADMIN")));
    }

    @AfterEach
    void clearContext() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    private TenantContextGuard createGuard(boolean isPostgreSQL) throws SQLException {
        DataSource ds = mock(DataSource.class);
        Connection conn = mock(Connection.class);
        DatabaseMetaData meta = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(conn);
        when(conn.getMetaData()).thenReturn(meta);
        when(meta.getDatabaseProductName()).thenReturn(isPostgreSQL ? "PostgreSQL" : "H2");
        return new TenantContextGuard(new StaticListableBeanFactory(Map.of("dataSource", ds)).getBeanProvider(DataSource.class));
    }

    @Test
    void anAnonymousCallerPassesSoTheSecurityChainAnswers401() throws Exception {
        SecurityContextHolder.clearContext();
        TenantContextGuard guard = createGuard(true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/corehr/devices");
        MockHttpServletResponse response = new MockHttpServletResponse();

        guard.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    // Con la ruta en pathInfo (servlet en "/*", MockMvc) getServletPath() viene vacío: no por eso es de tenant.
    @Test
    void aSaasPathThatArrivesAsPathInfoIsNotBlocked() throws Exception {
        TenantContextGuard guard = createGuard(true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("");
        request.setPathInfo("/saas/tenants");
        MockHttpServletResponse response = new MockHttpServletResponse();

        guard.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void aDatabaseErrorDoesNotSwitchTheGuardOffForGood() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection conn = mock(Connection.class);
        DatabaseMetaData meta = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenThrow(new SQLException("base caída")).thenReturn(conn);
        when(conn.getMetaData()).thenReturn(meta);
        when(meta.getDatabaseProductName()).thenReturn("PostgreSQL");
        TenantContextGuard guard = new TenantContextGuard(
                new StaticListableBeanFactory(Map.of("dataSource", ds)).getBeanProvider(DataSource.class));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/corehr/devices");

        guard.doFilterInternal(request, new MockHttpServletResponse(), chain);
        MockHttpServletResponse afterTheDatabaseIsBack = new MockHttpServletResponse();
        guard.doFilterInternal(request, afterTheDatabaseIsBack, chain);

        assertThat(afterTheDatabaseIsBack.getStatus()).isEqualTo(403);
    }

    @Test
    void withoutADataSourceTheGuardStandsAside() throws Exception {
        TenantContextGuard guard = new TenantContextGuard(new StaticListableBeanFactory().getBeanProvider(DataSource.class));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/corehr/devices");
        MockHttpServletResponse response = new MockHttpServletResponse();

        guard.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void request_toNonTenantPath_passesThrough() throws Exception {
        TenantContextGuard guard = createGuard(true);
        TenantContext.set("public");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        guard.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void request_toSaasPath_passesThrough() throws Exception {
        TenantContextGuard guard = createGuard(true);
        TenantContext.set("public");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/saas/tenants");
        MockHttpServletResponse response = new MockHttpServletResponse();

        guard.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void request_toTenantPathWithPublicSchema_returns403() throws Exception {
        TenantContextGuard guard = createGuard(true);
        TenantContext.set("public");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/incidentes");
        MockHttpServletResponse response = new MockHttpServletResponse();

        guard.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("Acceso denegado");
    }

    @Test
    void request_toTenantPathWithTenantSchema_passesThrough() throws Exception {
        TenantContextGuard guard = createGuard(true);
        TenantContext.set("tenant_demo");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/incidentes");
        MockHttpServletResponse response = new MockHttpServletResponse();

        guard.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void request_toTenantPathWithNullSchema_returns403() throws Exception {
        TenantContextGuard guard = createGuard(true);
        TenantContext.set(null);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/incidentes/tipos");
        MockHttpServletResponse response = new MockHttpServletResponse();

        guard.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void request_onH2_skipsGuard() throws Exception {
        TenantContextGuard guard = createGuard(false);
        TenantContext.set("public");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/incidentes");
        MockHttpServletResponse response = new MockHttpServletResponse();

        guard.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void request_toBranchPathWithPublicSchema_returns403() throws Exception {
        TenantContextGuard guard = createGuard(true);
        TenantContext.set("public");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/branches");
        MockHttpServletResponse response = new MockHttpServletResponse();

        guard.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void request_toActuatorPath_passesThrough() throws Exception {
        TenantContextGuard guard = createGuard(true);
        TenantContext.set("public");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        guard.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }
}
