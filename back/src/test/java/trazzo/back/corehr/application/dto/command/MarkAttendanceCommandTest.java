package trazzo.back.corehr.application.dto.command;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

class MarkAttendanceCommandTest {

    @Test
    void shouldConstructRecordWithAllFields() {
        var now = LocalDateTime.of(2026, 7, 19, 8, 30);
        var cmd = new MarkAttendanceCommand("tmpl", "aes", "iv", "tag", now, "DEV-01");
        assertEquals("tmpl", cmd.encryptedTemplateBase64());
        assertEquals("aes", cmd.encryptedAesKeyBase64());
        assertEquals("iv", cmd.ivBase64());
        assertEquals("tag", cmd.tagBase64());
        assertEquals(now, cmd.capturedAtUtc());
        assertEquals("DEV-01", cmd.deviceCode());
    }

    @Test
    void shouldHaveValueEquality() {
        var now = LocalDateTime.of(2026, 7, 19, 8, 30);
        var a = new MarkAttendanceCommand("t", "a", "i", "tag", now, "D1");
        var b = new MarkAttendanceCommand("t", "a", "i", "tag", now, "D1");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenFieldsDiffer() {
        var now = LocalDateTime.now();
        var a = new MarkAttendanceCommand("t1", "a", "i", "tag", now, "D1");
        var b = new MarkAttendanceCommand("t2", "a", "i", "tag", now, "D1");
        assertNotEquals(a, b);
    }
}
