package trazzo.back.shared.security;

import org.springframework.security.core.userdetails.UserDetails;

public interface TokenValidator {
    String extractUsername(String token);
    boolean isTokenValid(String token, UserDetails userDetails);
}
