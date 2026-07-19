package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

class BiometricIdentifyRequestTest {

    @Test
    void shouldConstructRecordWithAllFields() {
        var now = LocalDateTime.of(2026, 7, 19, 8, 30);
        var req = new BiometricIdentifyRequest(
                "ENROLL", "tmpl", "aes", "iv", "tag", now, "DEV-01", 42L, 100, 3
        );
        assertEquals("ENROLL", req.eventType());
        assertEquals("tmpl", req.encryptedTemplateBase64());
        assertEquals("aes", req.encryptedAesKeyBase64());
        assertEquals("iv", req.ivBase64());
        assertEquals("tag", req.tagBase64());
        assertEquals(now, req.capturedAtUtc());
        assertEquals("DEV-01", req.deviceCode());
        assertEquals(42L, req.tenantUserId());
        assertEquals(100, req.offlineEventId());
        assertEquals(3, req.retryCount());
    }

    @Test
    void shouldConstructWithNullOptionalFields() {
        var now = LocalDateTime.of(2026, 7, 19, 8, 30);
        var req = new BiometricIdentifyRequest(
                null, "tmpl", "aes", "iv", "tag", now, "DEV-01", null, null, null
        );
        assertNull(req.eventType());
        assertNull(req.tenantUserId());
        assertNull(req.offlineEventId());
        assertNull(req.retryCount());
    }

    @Test
    void shouldHaveValueEquality() {
        var now = LocalDateTime.of(2026, 7, 19, 8, 30);
        var a = new BiometricIdentifyRequest("E", "t", "a", "i", "tag", now, "D", 1L, 2, 0);
        var b = new BiometricIdentifyRequest("E", "t", "a", "i", "tag", now, "D", 1L, 2, 0);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenFieldsDiffer() {
        var now = LocalDateTime.now();
        var a = new BiometricIdentifyRequest("E", "t", "a", "i", "tag", now, "D1", null, null, null);
        var b = new BiometricIdentifyRequest("E", "t", "a", "i", "tag", now, "D2", null, null, null);
        assertNotEquals(a, b);
    }
}
