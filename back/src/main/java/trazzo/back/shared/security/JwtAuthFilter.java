package trazzo.back.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import trazzo.back.shared.tenancy.TenantContext;

import java.io.IOException;

@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final TokenValidator tokenValidator;
    private final UserDetailsService userDetailsService;

    public JwtAuthFilter(TokenValidator tokenValidator, UserDetailsService userDetailsService) {
        this.tokenValidator = tokenValidator;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Security: TenantContext drives DB schema routing downstream (Schema-per-tenant).
        // It must NEVER be set from an unvalidated token claim. Set it only AFTER the token
        // signature is verified AND all account flags pass, together with the SecurityContext.
        // Any failure path clears the ThreadLocal before continuing/aborting, and the outer
        // finally() is a safety net against cross-request leaks via Tomcat thread reuse.
        try {
            String header = request.getHeader("Authorization");

            if (header == null || !header.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            String token = header.substring(7);
            try {
                String username = tokenValidator.extractUsername(token);
                if (username == null || SecurityContextHolder.getContext().getAuthentication() != null) {
                    filterChain.doFilter(request, response);
                    return;
                }

                // Load account first; do NOT touch TenantContext until signature + flags pass.
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                boolean valid = tokenValidator.isTokenValid(token, userDetails)
                        && userDetails.isEnabled()
                        && userDetails.isAccountNonLocked()
                        && userDetails.isAccountNonExpired()
                        && userDetails.isCredentialsNonExpired();
                if (!valid) {
                    log.warn("Rejecting token — validation/account flags failed — path={}", request.getRequestURI());
                    abortUnauthorized(response);
                    return;
                }

                String tenantSchema = tokenValidator.extractTenantSchema(token);
                boolean isSaasAdmin = userDetails.getAuthorities().stream()
                        .anyMatch(a -> "ROLE_SAAS_ADMIN".equals(a.getAuthority()));
                if (!isSaasAdmin && (tenantSchema == null || tenantSchema.isBlank())) {
                    log.warn("Rejecting tenant-scoped user with missing tenant claim — path={}", request.getRequestURI());
                    abortUnauthorized(response);
                    return;
                }

                // All checks passed: NOW it is safe to bind the tenant context for the request.
                TenantContext.set(tenantSchema);
                var auth = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);

                filterChain.doFilter(request, response);
            } catch (Exception e) {
                log.warn("JWT auth failed [{}] — path={}", e.getClass().getSimpleName(), request.getRequestURI());
                TenantContext.clear();
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            }
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Fail-closed: abort the chain with 401 and clear all thread state. Used on every
     * authentication failure path so no downstream filter/controller may run with a
     * tenant schema or principal derived from an unvalidated token claim.
     */
    private void abortUnauthorized(HttpServletResponse response) {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }
}
