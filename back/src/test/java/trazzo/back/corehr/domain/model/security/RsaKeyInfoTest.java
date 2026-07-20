package trazzo.back.corehr.domain.model.security;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

class RsaKeyInfoTest {

    @Test
    void shouldConstructRecordWithAllFields() {
        var now = LocalDateTime.of(2026, 7, 19, 10, 30);
        var info = new RsaKeyInfo("-----BEGIN PUBLIC KEY-----", "key-1", now);
        assertEquals("-----BEGIN PUBLIC KEY-----", info.publicKeyPem());
        assertEquals("key-1", info.kid());
        assertEquals(now, info.createdAt());
    }

    @Test
    void shouldHaveValueEquality() {
        var now = LocalDateTime.of(2026, 1, 1, 0, 0);
        var a = new RsaKeyInfo("pem", "kid-1", now);
        var b = new RsaKeyInfo("pem", "kid-1", now);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenFieldsDiffer() {
        var now = LocalDateTime.of(2026, 1, 1, 0, 0);
        var a = new RsaKeyInfo("pem", "kid-1", now);
        var b = new RsaKeyInfo("pem", "kid-2", now);
        assertNotEquals(a, b);
    }

    @Test
    void shouldAllowNullFields() {
        var info = new RsaKeyInfo(null, null, null);
        assertNull(info.publicKeyPem());
        assertNull(info.kid());
        assertNull(info.createdAt());
    }
}
