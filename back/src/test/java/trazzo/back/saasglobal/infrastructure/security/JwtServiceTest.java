package trazzo.back.saasglobal.infrastructure.security;

import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = Base64.getEncoder()
            .encodeToString(Jwts.SIG.HS256.key().build().getEncoded());

    private final JwtService jwtService = new JwtService(SECRET, 86_400_000L);

    private UserDetails user(String username) {
        return User.withUsername(username).password("").authorities(List.of()).build();
    }

    @Test
    void generateToken_returnsNonBlankToken() {
        assertThat(jwtService.generateToken(user("user@test.com"))).isNotBlank();
    }

    @Test
    void extractUsername_returnsSubjectFromToken() {
        String token = jwtService.generateToken(user("user@test.com"));
        assertThat(jwtService.extractUsername(token)).isEqualTo("user@test.com");
    }

    @Test
    void isTokenValid_validToken_returnsTrue() {
        UserDetails u = user("user@test.com");
        assertThat(jwtService.isTokenValid(jwtService.generateToken(u), u)).isTrue();
    }

    @Test
    void isTokenValid_tokenForDifferentUser_returnsFalse() {
        String token = jwtService.generateToken(user("owner@test.com"));
        assertThat(jwtService.isTokenValid(token, user("other@test.com"))).isFalse();
    }

    @Test
    void getExpirationMs_returnsConfiguredValue() {
        assertThat(jwtService.getExpirationMs()).isEqualTo(86_400_000L);
    }
}
