package trazzo.back.audit.application.dto.result;

import org.junit.jupiter.api.Test;
import trazzo.back.audit.domain.model.tenant.SessionState;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SessionResultTest {

    @Test
    void shouldCreate() {
        var now = LocalDateTime.now();
        var result = new SessionResult(
                1L, "tenant-user-1", "hash123", "192.168.1.1",
                "Mozilla/5.0", "fp-abc-123",
                now, now.plusHours(1), now.plusHours(8), now.plusDays(1),
                SessionState.ACTIVE, now, now
        );
        assertEquals(1L, result.id());
        assertEquals("tenant-user-1", result.tenantUserId());
        assertEquals("hash123", result.refreshTokenHash());
        assertEquals("192.168.1.1", result.ipAddress());
        assertEquals("Mozilla/5.0", result.userAgent());
        assertEquals("fp-abc-123", result.deviceFingerprint());
        assertEquals(now, result.loginAt());
        assertEquals(now.plusHours(1), result.lasActivityAt());
        assertEquals(now.plusHours(8), result.logoutAt());
        assertEquals(now.plusDays(1), result.expiresAt());
        assertEquals(SessionState.ACTIVE, result.state());
        assertEquals(now, result.createdAt());
        assertEquals(now, result.updatedAt());
    }

    @Test
    void shouldHandleNullOptionalFields() {
        var now = LocalDateTime.now();
        var result = new SessionResult(
                1L, "tenant-user-1", "hash123", "192.168.1.1",
                null, null, now, null, null, now.plusDays(1),
                SessionState.ACTIVE, now, now
        );
        assertNull(result.userAgent());
        assertNull(result.deviceFingerprint());
        assertNull(result.lasActivityAt());
        assertNull(result.logoutAt());
    }

    @Test
    void shouldCreateWithExpiredState() {
        var now = LocalDateTime.now();
        var result = new SessionResult(
                1L, "tu-1", "hash", "10.0.0.1", null, null,
                now, null, null, now.minusHours(1),
                SessionState.EXPIRED, now, now
        );
        assertEquals(SessionState.EXPIRED, result.state());
    }

    @Test
    void shouldTestEquality() {
        var now = LocalDateTime.now();
        var r1 = new SessionResult(1L, "tu", "h", "ip", "ua", "fp", now, now, now, now, SessionState.ACTIVE, now, now);
        var r2 = new SessionResult(1L, "tu", "h", "ip", "ua", "fp", now, now, now, now, SessionState.ACTIVE, now, now);
        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void shouldTestToString() {
        var result = new SessionResult(null, "tu", null, null, null, null, null, null, null, null, SessionState.ACTIVE, null, null);
        assertTrue(result.toString().contains("tu"));
    }
}
