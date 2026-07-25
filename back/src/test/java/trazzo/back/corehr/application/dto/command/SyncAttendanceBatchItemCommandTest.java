package trazzo.back.corehr.application.dto.command;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

class SyncAttendanceBatchItemCommandTest {

    @Test
    void shouldConstructRecordWithAllFields() {
        var now = LocalDateTime.of(2026, 7, 19, 9, 0);
        var cmd = new SyncAttendanceBatchItemCommand("tmpl", "aes", "iv", "tag", now, "DEV-01", 100, 2);
        assertEquals("tmpl", cmd.encryptedTemplateBase64());
        assertEquals("aes", cmd.encryptedAesKeyBase64());
        assertEquals("iv", cmd.ivBase64());
        assertEquals("tag", cmd.tagBase64());
        assertEquals(now, cmd.capturedAtUtc());
        assertEquals("DEV-01", cmd.deviceCode());
        assertEquals(100, cmd.offlineEventId());
        assertEquals(2, cmd.retryCount());
    }

    @Test
    void shouldConstructWithNullOptionalFields() {
        var now = LocalDateTime.of(2026, 7, 19, 9, 0);
        var cmd = new SyncAttendanceBatchItemCommand("t", "a", "i", "tag", now, "D", null, null);
        assertNull(cmd.offlineEventId());
        assertNull(cmd.retryCount());
    }

    @Test
    void shouldHaveValueEquality() {
        var now = LocalDateTime.of(2026, 7, 19, 9, 0);
        var a = new SyncAttendanceBatchItemCommand("t", "a", "i", "tag", now, "D", 1, 0);
        var b = new SyncAttendanceBatchItemCommand("t", "a", "i", "tag", now, "D", 1, 0);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenFieldsDiffer() {
        var now = LocalDateTime.now();
        var a = new SyncAttendanceBatchItemCommand("t", "a", "i", "tag", now, "D", 1, 0);
        var b = new SyncAttendanceBatchItemCommand("t", "a", "i", "tag", now, "D", 2, 0);
        assertNotEquals(a, b);
    }
}
