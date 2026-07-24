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
        // Security model:
        //  - TenantContext drives DB schema routing downstream (Schema-per-tenant); it must
        //    never be set from an UNvalidated token claim.
        //  - extractUsername / extractTenantSchema both call parseClaims(token), which verifies
        //    the JWT signature (parseSignedClaims throws JwtException on bad signature). So once
        //    extractUsername returns (no exception), the token's signature is cryptographically
        //    trusted and its claims — including the tenant claim — are safe to read.
        //  - Therefore TenantContext.set(...) is permitted right after a successful
        //    extractUsername, BEFORE loadUserByUsername: the UserDetailsServiceImpl needs the
        //    tenant schema in the ThreadLocal to enrich authorities with tenant-scoped perms
        //    (findPermissionCodesByMasterUserId) against the right DB schema.
        //  - Every failure path (invalid signature, account flags, missing tenant claim, any
        //    exception) clears both TenantContext and SecurityContextHolder before aborting
        //    with 401, so no downstream filter/controller ever runs with a tenant schema or
        //    principal derived from an unvalidated token. The outer finally() is a safety net
        //    against cross-request leaks via Tomcat thread reuse.
        //  - IMPORTANT: filterChain.doFilter MUST run OUTSIDE the inner try/catch that guards
        //    JWT parsing. Otherwise exceptions thrown downstream (AccessDeniedException → 403,
        //    StorageException → 500, MethodArgumentNotValidException → 400, etc.) would be
        //    swallowed by the catch and converted to a misleading 401, masking the real status.
        try {
            String header = request.getHeader("Authorization");

            if (header == null || !header.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            String token = header.substring(7);
            try {
                // parseClaims(token) inside extractUsername verifies the JWT signature.
                // If the signature is invalid, it throws JwtException → caught below with 401.
                String username = tokenValidator.extractUsername(token);
                if (username == null || SecurityContextHolder.getContext().getAuthentication() != null) {
                    filterChain.doFilter(request, response);
                    return;
                }

                // Signature already verified by extractUsername; the tenant claim is now trusted.
                // Set the context BEFORE loadUserByUsername so UserDetailsServiceImpl can
                // resolve tenant-scoped permissions from the right schema (see L42-46 there).
                String tenantSchema = tokenValidator.extractTenantSchema(token);
                TenantContext.set(tenantSchema);

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

                boolean isSaasAdmin = userDetails.getAuthorities().stream()
                        .anyMatch(a -> "ROLE_SAAS_ADMIN".equals(a.getAuthority()));
                if (!isSaasAdmin && (tenantSchema == null || tenantSchema.isBlank())) {
                    log.warn("Rejecting tenant-scoped user with missing tenant claim — path={}", request.getRequestURI());
                    abortUnauthorized(response);
                    return;
                }

                // All checks passed: principal is authenticated and bound to the tenant schema.
                var auth = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception e) {
                // Only catches failures that occur DURING authentication setup (token parsing,
                // loadUserByUsername, isTokenValid). NOT downstream exceptions from the chain.
                log.warn("JWT auth failed [{}] — path={}", e.getClass().getSimpleName(), request.getRequestURI());
                abortUnauthorized(response);
                return;
            }

            // Run the chain OUTSIDE the inner try/catch so downstream exceptions (AccessDenied
            // → 403, StorageException → 500, validation → 400, etc.) propagate to the proper
            // ExceptionTranslationFilter / @ControllerAdvice with their real status, instead of
            // being masked as a 401 authentication failure.
            filterChain.doFilter(request, response);
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
