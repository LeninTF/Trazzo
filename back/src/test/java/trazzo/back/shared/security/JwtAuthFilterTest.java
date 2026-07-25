package trazzo.back.shared.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import trazzo.back.shared.tenancy.TenantContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class JwtAuthFilterTest {

    private final TokenValidator tokenValidator = mock(TokenValidator.class);
    private final UserDetailsService userDetailsService = mock(UserDetailsService.class);
    private final JwtAuthFilter filter = new JwtAuthFilter(tokenValidator, userDetailsService);

    private final FilterChain chain = mock(FilterChain.class);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    void request_withoutAuthorizationHeader_passesThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void request_withNonBearerHeader_passesThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void request_withValidBearerToken_setsAuthentication() throws Exception {
        UserDetails userDetails = User.withUsername("user@test.com")
                .password("").authorities(List.of()).build();
        when(tokenValidator.extractUsername("valid.token")).thenReturn("user@test.com");
        when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(userDetails);
        when(tokenValidator.isTokenValid("valid.token", userDetails)).thenReturn(true);
        when(tokenValidator.extractTenantSchema("valid.token")).thenReturn("tenant_acme");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
                .isEqualTo("user@test.com");
    }

    @Test
    void request_withSaasAdminTokenAndNoTenantClaim_stillSetsAuthentication() throws Exception {
        UserDetails admin = User.withUsername("admin@test.com")
                .password("").authorities(List.of(new SimpleGrantedAuthority("ROLE_SAAS_ADMIN"))).build();
        when(tokenValidator.extractUsername("admin.token")).thenReturn("admin@test.com");
        when(userDetailsService.loadUserByUsername("admin@test.com")).thenReturn(admin);
        when(tokenValidator.isTokenValid("admin.token", admin)).thenReturn(true);
        when(tokenValidator.extractTenantSchema("admin.token")).thenReturn(null);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer admin.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void request_withTenantUserTokenMissingTenantClaim_abortsChainWith401() throws Exception {
        UserDetails tenantUser = User.withUsername("user@test.com")
                .password("").authorities(List.of()).build();
        when(tokenValidator.extractUsername("token")).thenReturn("user@test.com");
        when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(tenantUser);
        when(tokenValidator.isTokenValid("token", tenantUser)).thenReturn(true);
        when(tokenValidator.extractTenantSchema("token")).thenReturn(null);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        verify(chain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(TenantContext.get()).isEqualTo(TenantContext.DEFAULT_SCHEMA);
    }

    @Test
    void request_withTenantUserTokenBlankTenantClaim_abortsChainWith401() throws Exception {
        UserDetails tenantUser = User.withUsername("user@test.com")
                .password("").authorities(List.of()).build();
        when(tokenValidator.extractUsername("token")).thenReturn("user@test.com");
        when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(tenantUser);
        when(tokenValidator.isTokenValid("token", tenantUser)).thenReturn(true);
        when(tokenValidator.extractTenantSchema("token")).thenReturn("   ");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        verify(chain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(TenantContext.get()).isEqualTo(TenantContext.DEFAULT_SCHEMA);
    }

    @Test
    void request_withInvalidToken_abortsChainWith401AndClearsTenantContext() throws Exception {
        when(tokenValidator.extractUsername("bad.token")).thenThrow(new RuntimeException("invalid"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer bad.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        verify(chain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(TenantContext.get()).isEqualTo(TenantContext.DEFAULT_SCHEMA);
    }

    @Test
    void request_whenTokenValidReturnsFalse_abortsChainWith401() throws Exception {
        UserDetails userDetails = User.withUsername("user@test.com")
                .password("").authorities(List.of()).build();
        when(tokenValidator.extractUsername("token")).thenReturn("user@test.com");
        when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(userDetails);
        when(tokenValidator.isTokenValid("token", userDetails)).thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        verify(chain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(TenantContext.get()).isEqualTo(TenantContext.DEFAULT_SCHEMA);
    }

    @Test
    void request_withValidTokenButDisabledUser_abortsChainWith401() throws Exception {
        UserDetails disabledUser = User.withUsername("disabled@test.com")
                .password("").authorities(List.of()).disabled(true).build();
        when(tokenValidator.extractUsername("valid.token")).thenReturn("disabled@test.com");
        when(userDetailsService.loadUserByUsername("disabled@test.com")).thenReturn(disabledUser);
        when(tokenValidator.isTokenValid("valid.token", disabledUser)).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        verify(chain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(TenantContext.get()).isEqualTo(TenantContext.DEFAULT_SCHEMA);
    }

    @Test
    void request_withTokenButNullUsername_passesThroughWithoutAuth() throws Exception {
        when(tokenValidator.extractUsername("empty.sub.token")).thenReturn(null);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer empty.sub.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(TenantContext.get()).isEqualTo(TenantContext.DEFAULT_SCHEMA);
        verifyNoInteractions(userDetailsService);
    }

    @Test
    void request_whenAlreadyAuthenticated_passesThroughSkippingJwt() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        "already@test.com", null, List.of()));
        when(tokenValidator.extractUsername("token")).thenReturn("user@test.com");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(userDetailsService);
    }

    @Test
    void request_whenLoadUserByUsernameThrows_abortsChainWith401AndClearsContext() throws Exception {
        when(tokenValidator.extractUsername("valid.sub.bad.backend.token")).thenReturn("user@test.com");
        when(tokenValidator.extractTenantSchema("valid.sub.bad.backend.token")).thenReturn("tenant_acme");
        when(userDetailsService.loadUserByUsername("user@test.com"))
                .thenThrow(new RuntimeException("DB glitch"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid.sub.bad.backend.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        verify(chain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        // TenantContext may have been set temporarily (extractTenantSchema succeeded), but the
        // abort path must clear it so downstream filters never see the tenant schema.
        assertThat(TenantContext.get()).isEqualTo(TenantContext.DEFAULT_SCHEMA);
    }

    @Test
    void request_withValidTokenButLockedAccount_abortsChainWith401() throws Exception {
        UserDetails lockedUser = User.withUsername("locked@test.com")
                .password("").authorities(List.of())
                .accountLocked(true).build();
        when(tokenValidator.extractUsername("valid.token")).thenReturn("locked@test.com");
        when(userDetailsService.loadUserByUsername("locked@test.com")).thenReturn(lockedUser);
        when(tokenValidator.isTokenValid("valid.token", lockedUser)).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        verify(chain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(TenantContext.get()).isEqualTo(TenantContext.DEFAULT_SCHEMA);
    }

    @Test
    void request_withValidTokenButExpiredCredentials_abortsChainWith401() throws Exception {
        UserDetails expiredCredsUser = User.withUsername("expired@test.com")
                .password("").authorities(List.of())
                .credentialsExpired(true).build();
        when(tokenValidator.extractUsername("valid.token")).thenReturn("expired@test.com");
        when(userDetailsService.loadUserByUsername("expired@test.com")).thenReturn(expiredCredsUser);
        when(tokenValidator.isTokenValid("valid.token", expiredCredsUser)).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        verify(chain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(TenantContext.get()).isEqualTo(TenantContext.DEFAULT_SCHEMA);
    }

    @Test
    void request_withValidTokenButAccountExpired_abortsChainWith401() throws Exception {
        UserDetails expiredUser = User.withUsername("expired@test.com")
                .password("").authorities(List.of())
                .accountExpired(true).build();
        when(tokenValidator.extractUsername("valid.token")).thenReturn("expired@test.com");
        when(userDetailsService.loadUserByUsername("expired@test.com")).thenReturn(expiredUser);
        when(tokenValidator.isTokenValid("valid.token", expiredUser)).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        verify(chain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(TenantContext.get()).isEqualTo(TenantContext.DEFAULT_SCHEMA);
    }

    @Test
    void request_whenIsTokenValidThrowsSignatureException_abortsChainAndClearsTenantSchema()
            throws Exception {
        UserDetails userDetails = User.withUsername("user@test.com")
                .password("").authorities(List.of()).build();
        // Note: extractUsername in this mock does NOT actually verify the signature (the real
        // JwtService does via parseClaims). Here we simulate the downstream isTokenValid()
        // raising a JwtException e.g. due to a node-side re-check failure. The contract under
        // test: TenantContext is set temporarily, but the abort path clears it, so the forged
        // tenant_schema never leaks downstream.
        when(tokenValidator.extractUsername("forged.token")).thenReturn("user@test.com");
        // Attacker forges a tenant_schema claim that must NEVER reach downstream.
        when(tokenValidator.extractTenantSchema("forged.token")).thenReturn("tenant_forged");
        when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(userDetails);
        when(tokenValidator.isTokenValid("forged.token", userDetails))
                .thenThrow(new io.jsonwebtoken.JwtException("bad signature"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer forged.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        verify(chain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        // Guardrail: the forged schema must have been cleared on abort — downstream never
        // observes tenant_forged.
        assertThat(TenantContext.get()).isEqualTo(TenantContext.DEFAULT_SCHEMA);
    }

    /**
     * Regression test for the 403 bug on tenant users (e.g. demo@trazzo.pe,
     * usuario@trazzo.pe) after the initial fail-closed fix.
     *
     * UserDetailsServiceImpl.loadUserByUsername reads TenantContext to enrich authorities
     * with tenant-scoped permission codes (findPermissionCodesByMasterUserId). If the
     * filter sets TenantContext only AFTER loadUserByUsername (as the first fix did),
     * those permissions are never loaded — so @PreAuthorize("hasAuthority(...)") on
     * endpoints like GET /incidentes returns 403 for valid tenant users.
     *
     * Contract: TenantContext.get() must be the tenant schema DURING loadUserByUsername,
     * but only when extractUsername (which verifies the signature) has succeeded.
     */
    @Test
    void request_setsTenantContextBeforeLoadUserByUsername_soTenantPermissionsLoad() throws Exception {
        // Capture the TenantContext value at the moment loadUserByUsername is invoked.
        java.util.concurrent.atomic.AtomicReference<String> schemaSeenByUserDetailsService =
                new java.util.concurrent.atomic.AtomicReference<>();
        UserDetails tenantUser = User.withUsername("demo@trazzo.pe")
                .password("").authorities(List.of(new SimpleGrantedAuthority("incidencias.ver-propias")))
                .build();
        when(tokenValidator.extractUsername("demo.token")).thenReturn("demo@trazzo.pe");
        when(tokenValidator.extractTenantSchema("demo.token")).thenReturn("tenant_demo");
        when(userDetailsService.loadUserByUsername("demo@trazzo.pe")).thenAnswer(inv -> {
            schemaSeenByUserDetailsService.set(TenantContext.get());
            return tenantUser;
        });
        when(tokenValidator.isTokenValid("demo.token", tenantUser)).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer demo.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
                .isEqualTo("demo@trazzo.pe");
        // The critical assertion: at the moment UserDetailsServiceImpl ran, the ThreadLocal
        // held the tenant schema — so findPermissionCodesByMasterUserId would have queried
        // the correct tenant DB. Without this, the user gets 403 on /incidentes.
        assertThat(schemaSeenByUserDetailsService.get())
                .as("TenantContext must be set BEFORE loadUserByUsername so tenant perms load")
                .isEqualTo("tenant_demo");
    }

    /**
     * Regression test for the 401-on-/storage/presigned-url bug.
     *
     * The fix v2 placed filterChain.doFilter INSIDE the inner try/catch that was meant to
     * handle only JWT parsing errors. As a consequence, any exception thrown downstream
     * (AccessDeniedException → 403, StorageException → 500 from LocalStorageStub when R2 is
     * not configured, MethodArgumentNotValidException → 400, etc.) was swallowed by the catch
     * and converted to a misleading 401. This manifested as 401 on /storage/presigned-url
     * for demo@trazzo.pe / usuario@trazzo.pe, even though /incidentes worked (it doesn't call
     * FileStoragePort).
     *
     * Contract: downstream exceptions must propagate unmodified — the filter must not catch
     * them, and must not overwrite the response status with 401.
     */
    @Test
    void request_whenDownstreamThrows_propagatesExceptionWithoutConvertingTo401() throws Exception {
        UserDetails userDetails = User.withUsername("demo@trazzo.pe")
                .password("").authorities(List.of(new SimpleGrantedAuthority("incidencias.crear")))
                .build();
        when(tokenValidator.extractUsername("demo.token")).thenReturn("demo@trazzo.pe");
        when(tokenValidator.extractTenantSchema("demo.token")).thenReturn("tenant_demo");
        when(userDetailsService.loadUserByUsername("demo@trazzo.pe")).thenReturn(userDetails);
        when(tokenValidator.isTokenValid("demo.token", userDetails)).thenReturn(true);

        // Simulate a downstream exception (e.g. StorageException from LocalStorageStub) bubbling
        // out of filterChain.doFilter — a real-world occurrence when R2 is not configured.
        RuntimeException downstream = new RuntimeException("StorageException: R2 not configured");
        doThrow(downstream).when(chain).doFilter(any(), any());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer demo.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // The downstream exception must propagate to the caller (Spring's
        // ExceptionTranslationFilter / @ControllerAdvice), NOT be swallowed by the filter.
        assertThatThrownBy(() -> filter.doFilterInternal(request, response, chain))
                .isSameAs(downstream);

        // The filter must NOT have overwritten the status with 401 (which would mask the real
        // 5xx error). The status stays as MockHttpServletResponse's default (200) because the
        // downstream layer never had a chance to set it — that's exactly what Spring expects;
        // it will apply the proper status itself via its exception handling pipeline.
        assertThat(response.getStatus())
                .as("filter must not overwrite downstream exception status with 401")
                .isNotEqualTo(401);
    }
}
