package trazzo.back.shared.security;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.UUID;

class AuthenticatedUserTest {

    @Test
    void shouldCreateAuthenticatedUser() {
        UUID id = UUID.randomUUID();
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        AuthenticatedUser user = new AuthenticatedUser(id, "admin@test.com", "pass", authorities, true);

        assertEquals(id, user.id());
        assertEquals("admin@test.com", user.getUsername());
        assertEquals("admin@test.com", user.email());
        assertEquals("pass", user.getPassword());
        assertEquals(authorities, user.getAuthorities());
        assertTrue(user.isEnabled());
        assertTrue(user.isAccountNonExpired());
        assertTrue(user.isAccountNonLocked());
        assertTrue(user.isCredentialsNonExpired());
    }

    @Test
    void shouldBeDisabledWhenEnabledIsFalse() {
        AuthenticatedUser user = new AuthenticatedUser(UUID.randomUUID(), "a@b.com", "p", List.of(), false);
        assertFalse(user.isEnabled());
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        UUID id = UUID.randomUUID();
        var auth1 = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        var auth2 = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        AuthenticatedUser u1 = new AuthenticatedUser(id, "a@b.com", "p1", auth1, true);
        AuthenticatedUser u2 = new AuthenticatedUser(id, "a@b.com", "p1", auth2, true);
        assertEquals(u1, u2);
        assertEquals(u1.hashCode(), u2.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenDifferentId() {
        var auth = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        AuthenticatedUser u1 = new AuthenticatedUser(UUID.randomUUID(), "a@b.com", "p", auth, true);
        AuthenticatedUser u2 = new AuthenticatedUser(UUID.randomUUID(), "a@b.com", "p", auth, true);
        assertNotEquals(u1, u2);
    }

    @Test
    void shouldReturnToString() {
        AuthenticatedUser user = new AuthenticatedUser(UUID.randomUUID(), "a@b.com", "p", List.of(), true);
        assertNotNull(user.toString());
        assertTrue(user.toString().contains("a@b.com"));
    }
}
