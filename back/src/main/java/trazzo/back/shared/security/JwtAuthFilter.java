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
        // TenantContext must stay set for the full downstream chain (controllers/repositories
        // borrow connections during filterChain.doFilter) and always be cleared afterward —
        // Tomcat reuses this thread for unrelated requests once this one completes.
        try {
            String header = request.getHeader("Authorization");

            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);
                try {
                    String username = tokenValidator.extractUsername(token);

                    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                        if (tokenValidator.isTokenValid(token, userDetails)
                                && userDetails.isEnabled()
                                && userDetails.isAccountNonLocked()
                                && userDetails.isAccountNonExpired()
                                && userDetails.isCredentialsNonExpired()) {
                            var auth = new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(auth);
                            TenantContext.set(tokenValidator.extractTenantSchema(token));
                        }
                    }
                } catch (Exception e) {
                    log.warn("JWT auth failed [{}] — path={}", e.getClass().getSimpleName(), request.getRequestURI());
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
