package trazzo.back.audit.application.dto.result;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TenantSettingsRecordResultTest {

    @Test
    void shouldCreate() {
        var now = LocalDateTime.now();
        var result = new TenantSettingsRecordResult(
                1L, "ts-1", "trazzo_db", "db.example.com",
                "admin", "user-1", "Schema migration v2", now
        );
        assertEquals(1L, result.id());
        assertEquals("ts-1", result.tenantSettingId());
        assertEquals("trazzo_db", result.dbName());
        assertEquals("db.example.com", result.dbHost());
        assertEquals("admin", result.dbUser());
        assertEquals("user-1", result.userId());
        assertEquals("Schema migration v2", result.changeReason());
        assertEquals(now, result.createdAt());
    }

    @Test
    void shouldHandleNullFields() {
        var result = new TenantSettingsRecordResult(
                null, null, null, null,
                null, null, null, null
        );
        assertNull(result.id());
        assertNull(result.tenantSettingId());
        assertNull(result.dbName());
        assertNull(result.dbHost());
        assertNull(result.dbUser());
        assertNull(result.userId());
        assertNull(result.changeReason());
        assertNull(result.createdAt());
    }

    @Test
    void shouldTestEquality() {
        var now = LocalDateTime.now();
        var r1 = new TenantSettingsRecordResult(1L, "ts", "db", "host", "user", "uid", "reason", now);
        var r2 = new TenantSettingsRecordResult(1L, "ts", "db", "host", "user", "uid", "reason", now);
        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void shouldTestToString() {
        var result = new TenantSettingsRecordResult(null, "ts-1", null, null, null, null, null, null);
        assertTrue(result.toString().contains("ts-1"));
    }
}
