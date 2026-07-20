package trazzo.back.shared.security;

import org.springframework.security.core.userdetails.UserDetails;

public interface TokenValidator {
    String extractUsername(String token);
    boolean isTokenValid(String token, UserDetails userDetails);

    /** Returns the tenant schema claim, or null for SaaS-admin tokens (no tenant). */
    String extractTenantSchema(String token);
}
