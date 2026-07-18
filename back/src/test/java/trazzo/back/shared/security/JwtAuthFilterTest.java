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
    void request_withTenantUserTokenMissingTenantClaim_rejectsAuthentication() throws Exception {
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

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void request_withTenantUserTokenBlankTenantClaim_rejectsAuthentication() throws Exception {
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

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void request_withInvalidToken_passesThrough() throws Exception {
        when(tokenValidator.extractUsername("bad.token")).thenThrow(new RuntimeException("invalid"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer bad.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void request_whenTokenValidReturnsFalse_doesNotSetAuthentication() throws Exception {
        UserDetails userDetails = User.withUsername("user@test.com")
                .password("").authorities(List.of()).build();
        when(tokenValidator.extractUsername("token")).thenReturn("user@test.com");
        when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(userDetails);
        when(tokenValidator.isTokenValid("token", userDetails)).thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void request_withValidTokenButDisabledUser_doesNotSetAuthentication() throws Exception {
        UserDetails disabledUser = User.withUsername("disabled@test.com")
                .password("").authorities(List.of()).disabled(true).build();
        when(tokenValidator.extractUsername("valid.token")).thenReturn("disabled@test.com");
        when(userDetailsService.loadUserByUsername("disabled@test.com")).thenReturn(disabledUser);
        when(tokenValidator.isTokenValid("valid.token", disabledUser)).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
