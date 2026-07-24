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
        when(userDetailsService.loadUserByUsername("user@test.com"))
                .thenThrow(new RuntimeException("DB glitch"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid.sub.bad.backend.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        verify(chain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(TenantContext.get()).isEqualTo(TenantContext.DEFAULT_SCHEMA);
        // Critical: extractTenantSchema must NEVER have been invoked (no set() opportunity).
        verify(tokenValidator, never()).extractTenantSchema(any());
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
    void request_whenIsTokenValidThrowsSignatureException_abortsChainAndDoesNotSetTenantSchema()
            throws Exception {
        UserDetails userDetails = User.withUsername("user@test.com")
                .password("").authorities(List.of()).build();
        when(tokenValidator.extractUsername("forged.token")).thenReturn("user@test.com");
        when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(userDetails);
        when(tokenValidator.isTokenValid("forged.token", userDetails))
                .thenThrow(new io.jsonwebtoken.JwtException("bad signature"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        // Attacker forges a tenant_schema claim that should NEVER be honored.
        request.addHeader("Authorization", "Bearer forged.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        verify(chain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(TenantContext.get()).isEqualTo(TenantContext.DEFAULT_SCHEMA);
        // Verify the guardrail: TenantContext.set was never called with the forged schema.
        verify(tokenValidator, never()).extractTenantSchema(any());
    }
}
