package trazzo.back.audit.application.dto.result;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AuditLogResultTest {

    @Test
    void shouldCreate() {
        var now = LocalDateTime.now();
        var oldValue = Map.<String, Object>of("name", "old");
        var newValue = Map.<String, Object>of("name", "new");
        var result = new AuditLogResult(
                "1", "evt-1", now, "tenant-1", "t-1",
                "jdoe", "jdoe@test.com", "UPDATE", "AUDIT",
                "User", "42", "192.168.1.1", "Mozilla/5.0",
                oldValue, newValue
        );
        assertEquals("1", result.id());
        assertEquals("evt-1", result.eventId());
        assertEquals(now, result.fecha());
        assertEquals("tenant-1", result.tenant());
        assertEquals("t-1", result.tenantId());
        assertEquals("jdoe", result.userName());
        assertEquals("jdoe@test.com", result.userEmail());
        assertEquals("UPDATE", result.accion());
        assertEquals("AUDIT", result.tipo());
        assertEquals("User", result.entidad());
        assertEquals("42", result.entidadId());
        assertEquals("192.168.1.1", result.ipAddress());
        assertEquals("Mozilla/5.0", result.userAgent());
        assertEquals(oldValue, result.oldValue());
        assertEquals(newValue, result.newValue());
    }

    @Test
    void shouldHandleNullValues() {
        var result = new AuditLogResult(
                null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, null, null
        );
        assertNull(result.id());
        assertNull(result.eventId());
        assertNull(result.fecha());
        assertNull(result.tenant());
        assertNull(result.tenantId());
        assertNull(result.userName());
        assertNull(result.userEmail());
        assertNull(result.accion());
        assertNull(result.tipo());
        assertNull(result.entidad());
        assertNull(result.entidadId());
        assertNull(result.ipAddress());
        assertNull(result.userAgent());
        assertNull(result.oldValue());
        assertNull(result.newValue());
    }

    @Test
    void shouldTestEquality() {
        var now = LocalDateTime.now();
        var r1 = new AuditLogResult("1", "e1", now, "t", "tid", "u", "m", "a", "tp", "en", "eid", "ip", "ua", null, null);
        var r2 = new AuditLogResult("1", "e1", now, "t", "tid", "u", "m", "a", "tp", "en", "eid", "ip", "ua", null, null);
        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void shouldTestToString() {
        var result = new AuditLogResult("1", "e1", null, "t", null, null, null, null, null, null, null, null, null, null, null);
        assertTrue(result.toString().contains("e1"));
    }
}
