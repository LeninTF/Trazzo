package trazzo.back.shared.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import trazzo.back.shared.tenancy.TenantContext;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DeviceTokenAuthFilterTest {

    private static final String TOKEN = "trzd_" + "A".repeat(43);
    private static final DeviceAgentPrincipal AGENT =
            new DeviceAgentPrincipal(UUID.randomUUID(), "tenant_demo", 7L, "DVC-007");

    private final AgentDeviceTokenRepository tokens = mock(AgentDeviceTokenRepository.class);
    private final DeviceTokenAuthFilter filter = new DeviceTokenAuthFilter(tokens);
    private final FilterChain chain = mock(FilterChain.class);

    @AfterEach
    void clearContexts() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    private static MockHttpServletRequest request(String method, String path, String authorization) {
        var request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        if (authorization != null) {
            request.addHeader("Authorization", authorization);
        }
        return request;
    }

    @Test
    void aRequestWithoutADeviceTokenPassesThroughUntouched() throws Exception {
        var request = request("GET", "/corehr/devices", "Bearer eyJhbGciOiJIUzI1NiJ9.e30.firma");
        var response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(tokens);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void aValidTokenAuthenticatesTheAgentInsideItsTenant() throws Exception {
        when(tokens.findActive(TOKEN)).thenReturn(Optional.of(AGENT));
        var request = request("POST", "/asistencia/marcar", "Bearer " + TOKEN);
        var response = new MockHttpServletResponse();
        AtomicReference<String> schemaSeen = new AtomicReference<>();
        AtomicReference<Authentication> authSeen = new AtomicReference<>();
        FilterChain capturing = (req, res) -> {
            schemaSeen.set(TenantContext.get());
            authSeen.set(SecurityContextHolder.getContext().getAuthentication());
        };

        filter.doFilterInternal(request, response, capturing);

        assertThat(schemaSeen.get()).isEqualTo("tenant_demo");
        assertThat(authSeen.get().getPrincipal()).isEqualTo(AGENT);
        assertThat(authSeen.get().getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_AGENT");
        assertThat(TenantContext.get()).isEqualTo(TenantContext.DEFAULT_SCHEMA);
    }

    @Test
    void anUnknownOrRevokedTokenIs401() throws Exception {
        when(tokens.findActive(TOKEN)).thenReturn(Optional.empty());
        var response = new MockHttpServletResponse();

        filter.doFilterInternal(request("POST", "/asistencia/marcar", "Bearer " + TOKEN), response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(any(), any());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void aPathOutsideTheAgentEndpointsIs403() throws Exception {
        when(tokens.findActive(TOKEN)).thenReturn(Optional.of(AGENT));
        var response = new MockHttpServletResponse();

        filter.doFilterInternal(request("GET", "/corehr/devices", "Bearer " + TOKEN), response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        verify(chain, never()).doFilter(any(), any());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void theRightPathWithAnotherMethodIs403() throws Exception {
        when(tokens.findActive(TOKEN)).thenReturn(Optional.of(AGENT));
        var response = new MockHttpServletResponse();

        filter.doFilterInternal(request("GET", "/asistencia/marcar", "Bearer " + TOKEN), response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
    }

    @ParameterizedTest
    @CsvSource({
            "POST, /asistencia/marcar",
            "POST, /asistencia/sync",
            "GET, /corehr/biometria/enroll/pendiente",
            "POST, /corehr/biometria/enroll/completar",
            "GET, /security/public-key"
    })
    void everyAgentEndpointIsAllowed(String method, String path) throws Exception {
        when(tokens.findActive(TOKEN)).thenReturn(Optional.of(AGENT));
        var request = request(method, path, "Bearer " + TOKEN);
        var response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    // Con el DispatcherServlet mapeado como "/*" (y en MockMvc) la ruta llega en pathInfo, no en servletPath.
    @Test
    void anAgentEndpointThatArrivesAsPathInfoIsAlsoRecognised() throws Exception {
        when(tokens.findActive(TOKEN)).thenReturn(Optional.of(AGENT));
        var request = new MockHttpServletRequest("POST", "/asistencia/marcar");
        request.setServletPath("");
        request.setPathInfo("/asistencia/marcar");
        request.addHeader("Authorization", "Bearer " + TOKEN);
        var response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void aDatabaseFailureIs503AndAuthenticatesNobody() throws Exception {
        when(tokens.findActive(TOKEN)).thenThrow(new DataAccessResourceFailureException("db down"));
        var response = new MockHttpServletResponse();

        filter.doFilterInternal(request("POST", "/asistencia/marcar", "Bearer " + TOKEN), response, chain);

        assertThat(response.getStatus()).isEqualTo(503);
        verify(chain, never()).doFilter(any(), any());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
