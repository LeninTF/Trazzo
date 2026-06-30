package trazzo.back.audit.domain.model.master;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

class TenantSettingsRecordTest {

    @Test
    void shouldCreateTenantSettingsRecord() {
        var now = LocalDateTime.now();
        var r = new TenantSettingsRecord(1L, "ts-1", "db1", "host1",
                "user1", "pass1", "admin", "Updated config", now);
        assertEquals(1L, r.getId());
        assertEquals("admin", r.getUserId());
        assertEquals("Updated config", r.getChangeReason());
    }

    @Test
    void shouldThrowWhenUserIdIsNull() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> new TenantSettingsRecord(1L, "ts-1", "db1", "host1",
                        "user1", "pass1", null, "reason", LocalDateTime.now()));
        assertEquals("User id is required", ex.getMessage());
    }

    @Test
    void shouldThrowWhenUserIdIsBlank() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> new TenantSettingsRecord(1L, "ts-1", "db1", "host1",
                        "user1", "pass1", "  ", "reason", LocalDateTime.now()));
        assertEquals("User id is required", ex.getMessage());
    }

    @Test
    void shouldThrowWhenChangeReasonIsNull() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> new TenantSettingsRecord(1L, "ts-1", "db1", "host1",
                        "user1", "pass1", "admin", null, LocalDateTime.now()));
        assertEquals("Change reason is required", ex.getMessage());
    }

    @Test
    void shouldThrowWhenChangeReasonIsBlank() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> new TenantSettingsRecord(1L, "ts-1", "db1", "host1",
                        "user1", "pass1", "admin", "", LocalDateTime.now()));
        assertEquals("Change reason is required", ex.getMessage());
    }

    @Test
    void shouldReturnTrueWhenHasChangeReason() {
        var r = new TenantSettingsRecord(1L, "ts-1", "db1", "host1",
                "user1", "pass1", "admin", "reason", LocalDateTime.now());
        assertTrue(r.hasChangeReason());
    }

    @Test
    void shouldReturnFalseWhenHasChangeReasonIsBlank() {
        var r = new TenantSettingsRecord(1L, "ts-1", "db1", "host1",
                "user1", "pass1", "admin", "reason", LocalDateTime.now());
        assertTrue(r.hasChangeReason());
    }

    @Test
    void shouldReturnTrueWhenBelongsToUser() {
        var r = new TenantSettingsRecord(1L, "ts-1", "db1", "host1",
                "user1", "pass1", "admin", "reason", LocalDateTime.now());
        assertTrue(r.belongsTo("admin"));
    }

    @Test
    void shouldReturnFalseWhenNotBelongsToUser() {
        var r = new TenantSettingsRecord(1L, "ts-1", "db1", "host1",
                "user1", "pass1", "admin", "reason", LocalDateTime.now());
        assertFalse(r.belongsTo("other"));
    }

}
