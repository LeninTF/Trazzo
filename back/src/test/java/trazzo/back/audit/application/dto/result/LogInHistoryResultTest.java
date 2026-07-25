package trazzo.back.audit.application.dto.result;

import org.junit.jupiter.api.Test;
import trazzo.back.audit.domain.model.master.StatusLogin;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class LogInHistoryResultTest {

    @Test
    void shouldCreateWithSuccessStatus() {
        var now = LocalDateTime.now();
        var result = new LogInHistoryResult(
                "1", "user-1", "jdoe@test.com", StatusLogin.SUCCESS,
                "192.168.1.1", "Mozilla/5.0", now
        );
        assertEquals("1", result.id());
        assertEquals("user-1", result.userId());
        assertEquals("jdoe@test.com", result.attemptedEmail());
        assertEquals(StatusLogin.SUCCESS, result.status());
        assertEquals("192.168.1.1", result.ipAddress());
        assertEquals("Mozilla/5.0", result.userAgent());
        assertEquals(now, result.createdAt());
    }

    @Test
    void shouldCreateWithFailedStatus() {
        var now = LocalDateTime.now();
        var result = new LogInHistoryResult(
                "2", "user-2", "bad@test.com", StatusLogin.FAILED_WRONG_PASSWORD,
                "10.0.0.1", "curl/7.68", now
        );
        assertEquals(StatusLogin.FAILED_WRONG_PASSWORD, result.status());
    }

    @Test
    void shouldCreateWithLockedOutStatus() {
        var now = LocalDateTime.now();
        var result = new LogInHistoryResult(
                "3", "user-3", "locked@test.com", StatusLogin.LOCKED_OUT,
                "10.0.0.1", null, now
        );
        assertEquals(StatusLogin.LOCKED_OUT, result.status());
        assertNull(result.userAgent());
    }

    @Test
    void shouldHandleNullId() {
        var now = LocalDateTime.now();
        var result = new LogInHistoryResult(
                null, "user-1", "test@test.com", StatusLogin.SUCCESS,
                "127.0.0.1", "agent", now
        );
        assertNull(result.id());
    }

    @Test
    void shouldTestEquality() {
        var now = LocalDateTime.now();
        var r1 = new LogInHistoryResult("1", "u", "e@m.com", StatusLogin.SUCCESS, "ip", "ua", now);
        var r2 = new LogInHistoryResult("1", "u", "e@m.com", StatusLogin.SUCCESS, "ip", "ua", now);
        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void shouldTestToString() {
        var result = new LogInHistoryResult(null, "u", null, StatusLogin.LOGOUT_EXPLICIT, null, null, null);
        assertTrue(result.toString().contains("LOGOUT_EXPLICIT"));
    }
}
