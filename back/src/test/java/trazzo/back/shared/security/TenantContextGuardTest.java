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
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import trazzo.back.shared.tenancy.TenantContext;

class TenantContextGuardTest {

    private final FilterChain chain = mock(FilterChain.class);

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    private TenantContextGuard createGuard(boolean isPostgreSQL) throws SQLException {
        DataSource ds = mock(DataSource.class);
        Connection conn = mock(Connection.class);
        DatabaseMetaData meta = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(conn);
        when(conn.getMetaData()).thenReturn(meta);
        when(meta.getDatabaseProductName()).thenReturn(isPostgreSQL ? "PostgreSQL" : "H2");
        return new TenantContextGuard(ds);
    }

    @Test
    void request_toNonTenantPath_passesThrough() throws Exception {
        TenantContextGuard guard = createGuard(true);
        TenantContext.set("public");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/auth/login");
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
        request.setRequestURI("/saas/tenants");
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
        request.setRequestURI("/incidentes");
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
        request.setRequestURI("/incidentes");
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
        request.setRequestURI("/incidentes/tipos");
        MockHttpServletResponse response = new MockHttpServletResponse();

        guard.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void request_onH2_skipsGuard() throws Exception {
        TenantContextGuard guard = createGuard(false);
        TenantContext.set("public");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/incidentes");
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
        request.setRequestURI("/branches");
        MockHttpServletResponse response = new MockHttpServletResponse();

        guard.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void request_toActuatorPath_passesThrough() throws Exception {
        TenantContextGuard guard = createGuard(true);
        TenantContext.set("public");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        guard.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }
}
