package trazzo.back.audit.application.dto.result;

import org.junit.jupiter.api.Test;
import trazzo.back.audit.domain.model.master.Action;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AuditLogDetailResultTest {

    @Test
    void shouldCreate() {
        var now = LocalDateTime.now();
        var previousValue = Map.<String, Object>of("field", "old");
        var newValue = Map.<String, Object>of("field", "new");
        var result = new AuditLogDetailResult(
                1L, "User", "42", Action.UPDATE, "user-1",
                "/api/users/42", "192.168.1.1", "curl/7.68",
                previousValue, newValue, now
        );
        assertEquals(1L, result.id());
        assertEquals("User", result.entity());
        assertEquals("42", result.entityId());
        assertEquals(Action.UPDATE, result.action());
        assertEquals("user-1", result.userId());
        assertEquals("/api/users/42", result.endpoint());
        assertEquals("192.168.1.1", result.ipAdress());
        assertEquals("curl/7.68", result.userAgent());
        assertEquals(previousValue, result.previousValue());
        assertEquals(newValue, result.newValue());
        assertEquals(now, result.createdAt());
    }

    @Test
    void shouldHandleNullMaps() {
        var now = LocalDateTime.now();
        var result = new AuditLogDetailResult(
                1L, "User", "42", Action.CREATE, "user-1",
                "/api/users", "10.0.0.1", null, null, null, now
        );
        assertNull(result.userAgent());
        assertNull(result.previousValue());
        assertNull(result.newValue());
    }

    @Test
    void shouldTestEquality() {
        var now = LocalDateTime.now();
        var r1 = new AuditLogDetailResult(1L, "E", "1", Action.DELETE, "u", "/e", "ip", "ua", null, null, now);
        var r2 = new AuditLogDetailResult(1L, "E", "1", Action.DELETE, "u", "/e", "ip", "ua", null, null, now);
        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void shouldTestToString() {
        var result = new AuditLogDetailResult(null, "User", null, Action.CREATE, null, null, null, null, null, null, null);
        assertTrue(result.toString().contains("User"));
    }
}
