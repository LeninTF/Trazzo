package trazzo.back.audit.domain.model.tenant;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

class SesionTest {

    private final LocalDateTime now = LocalDateTime.now();

    @Test
    void shouldCreateActiveSession() {
        var s = new Session(1L, "tenant-user-1", "hash123", "10.0.0.1",
                "Mozilla/5.0", "fp-abc", now, now, null,
                now.plusHours(2), SessionState.ACTIVE, now, now);
        assertEquals(1L, s.getId());
        assertEquals("tenant-user-1", s.getTenantUserId());
        assertEquals("hash123", s.getRefreshTokenHash());
        assertEquals("10.0.0.1", s.getIpAddress());
        assertEquals("Mozilla/5.0", s.getUserAgent());
        assertEquals("fp-abc", s.getDeviceFingerprint());
        assertEquals(now, s.getLoginAt());
        assertEquals(now, s.getLasActivityAt());
        assertNull(s.getLogoutAt());
        assertEquals(now.plusHours(2), s.getExpiresAt());
        assertEquals(SessionState.ACTIVE, s.getState());
        assertEquals(now, s.getCreatedAt());
        assertEquals(now, s.getUpdatedAt());
    }

    @Test
    void shouldCreateLoggedOutSession() {
        var loginAt = now.minusDays(1);
        var logoutAt = now.minusHours(1);
        var s = new Session(1L, "tenant-user-1", "hash123", "10.0.0.1",
                "Mozilla/5.0", null, loginAt, loginAt, logoutAt,
                loginAt.plusHours(2), SessionState.LOGGED_OUT, now, now);
        assertEquals(logoutAt, s.getLogoutAt());
        assertEquals(SessionState.LOGGED_OUT, s.getState());
    }

    @Test
    void shouldCreateExpiredSession() {
        var loginAt = now.minusDays(2);
        var expiresAt = now.minusDays(1);
        var s = new Session(1L, "tenant-user-1", "hash123", "10.0.0.1",
                "Mozilla/5.0", null, loginAt, loginAt, null,
                expiresAt, SessionState.EXPIRED, now, now);
        assertEquals(expiresAt, s.getExpiresAt());
        assertEquals(SessionState.EXPIRED, s.getState());
    }

    @Test
    void shouldThrowWhenTenantUserIdIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new Session(1L, null, "hash", "ip", "ua", null,
                        now, now, null, now.plusHours(1),
                        SessionState.ACTIVE, now, now));
    }

    @Test
    void shouldThrowWhenTenantUserIdIsBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> new Session(1L, " ", "hash", "ip", "ua", null,
                        now, now, null, now.plusHours(1),
                        SessionState.ACTIVE, now, now));
    }

    @Test
    void shouldThrowWhenRefreshTokenHashIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new Session(1L, "user-1", null, "ip", "ua", null,
                        now, now, null, now.plusHours(1),
                        SessionState.ACTIVE, now, now));
    }

    @Test
    void shouldThrowWhenRefreshTokenHashIsBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> new Session(1L, "user-1", "", "ip", "ua", null,
                        now, now, null, now.plusHours(1),
                        SessionState.ACTIVE, now, now));
    }

    @Test
    void shouldThrowWhenIpAddressIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new Session(1L, "user-1", "hash", null, "ua", null,
                        now, now, null, now.plusHours(1),
                        SessionState.ACTIVE, now, now));
    }

    @Test
    void shouldThrowWhenIpAddressIsBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> new Session(1L, "user-1", "hash", "", "ua", null,
                        now, now, null, now.plusHours(1),
                        SessionState.ACTIVE, now, now));
    }

    @Test
    void shouldThrowWhenUserAgentIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new Session(1L, "user-1", "hash", "ip", null, null,
                        now, now, null, now.plusHours(1),
                        SessionState.ACTIVE, now, now));
    }

    @Test
    void shouldThrowWhenUserAgentIsBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> new Session(1L, "user-1", "hash", "ip", "", null,
                        now, now, null, now.plusHours(1),
                        SessionState.ACTIVE, now, now));
    }

    @Test
    void shouldThrowWhenLoginAtIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new Session(1L, "user-1", "hash", "ip", "ua", null,
                        null, now, null, now.plusHours(1),
                        SessionState.ACTIVE, now, now));
    }

    @Test
    void shouldThrowWhenStateIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new Session(1L, "user-1", "hash", "ip", "ua", null,
                        now, now, null, now.plusHours(1),
                        null, now, now));
    }

    @Test
    void shouldThrowWhenExpiresAtIsBeforeLoginAt() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> new Session(1L, "user-1", "hash", "ip", "ua", null,
                        now, now, null, now.minusHours(1),
                        SessionState.ACTIVE, now, now));
        assertTrue(ex.getMessage().contains("before"));
    }

    @Test
    void shouldThrowWhenExpiresAtEqualsLoginAt() {
        assertThrows(IllegalArgumentException.class,
                () -> new Session(1L, "user-1", "hash", "ip", "ua", null,
                        now, now, null, now,
                        SessionState.ACTIVE, now, now));
    }

    @Test
    void shouldThrowWhenLogoutAtIsBeforeLoginAt() {
        assertThrows(IllegalArgumentException.class,
                () -> new Session(1L, "user-1", "hash", "ip", "ua", null,
                        now, now, now.minusHours(1), now.plusHours(1),
                        SessionState.ACTIVE, now, now));
    }

    @Test
    void shouldThrowWhenLasActivityAtIsBeforeLoginAt() {
        assertThrows(IllegalArgumentException.class,
                () -> new Session(1L, "user-1", "hash", "ip", "ua", null,
                        now, now.minusHours(1), null, now.plusHours(1),
                        SessionState.ACTIVE, now, now));
    }

    @Test
    void shouldThrowWhenLoggedOutAndLogoutAtIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new Session(1L, "user-1", "hash", "ip", "ua", null,
                        now, now, null, now.plusHours(1),
                        SessionState.LOGGED_OUT, now, now));
    }

    @Test
    void shouldThrowWhenActiveAndLogoutAtIsNotNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new Session(1L, "user-1", "hash", "ip", "ua", null,
                        now, now, now, now.plusHours(1),
                        SessionState.ACTIVE, now, now));
    }

    @Test
    void shouldThrowWhenExpiredAndNotYetExpired() {
        assertThrows(IllegalArgumentException.class,
                () -> new Session(1L, "user-1", "hash", "ip", "ua", null,
                        now, now, null, now.plusHours(1),
                        SessionState.EXPIRED, now, now));
    }

}
