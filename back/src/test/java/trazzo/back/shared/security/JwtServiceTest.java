package trazzo.back.shared.security;

import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final JwtService JWT_SERVICE;

    static {
        var key = Jwts.SIG.HS256.key().build();
        var secret = Base64.getEncoder().encodeToString(key.getEncoded());
        JWT_SERVICE = new JwtService(secret, 3_600_000L);
    }

    private static AuthenticatedUser stubUser() {
        return new AuthenticatedUser(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "user@test.com",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")),
                true
        );
    }

    @Test
    void generateToken_returnsNonBlankToken() {
        assertThat(JWT_SERVICE.generateToken(stubUser())).isNotBlank();
    }

    @Test
    void extractEmail_returnsEmailEmbeddedInToken() {
        var token = JWT_SERVICE.generateToken(stubUser());
        assertThat(JWT_SERVICE.extractEmail(token)).isEqualTo("user@test.com");
    }

    @Test
    void isTokenValid_validToken_returnsTrue() {
        var token = JWT_SERVICE.generateToken(stubUser());
        assertThat(JWT_SERVICE.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_tamperedToken_returnsFalse() {
        var token = JWT_SERVICE.generateToken(stubUser());
        assertThat(JWT_SERVICE.isTokenValid(token + "tampered")).isFalse();
    }

    @Test
    void isTokenValid_randomString_returnsFalse() {
        assertThat(JWT_SERVICE.isTokenValid("not.a.jwt")).isFalse();
    }

    @Test
    void isTokenValid_expiredToken_returnsFalse() {
        var key = Jwts.SIG.HS256.key().build();
        var secret = Base64.getEncoder().encodeToString(key.getEncoded());
        var expiredService = new JwtService(secret, -1_000L);
        var token = expiredService.generateToken(stubUser());
        assertThat(expiredService.isTokenValid(token)).isFalse();
    }

    @Test
    void getExpirationMs_returnsConfiguredValue() {
        assertThat(JWT_SERVICE.getExpirationMs()).isEqualTo(3_600_000L);
    }
}
