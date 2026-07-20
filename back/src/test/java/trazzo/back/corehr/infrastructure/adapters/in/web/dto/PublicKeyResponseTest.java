package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import trazzo.back.corehr.application.port.out.CryptoKeyProviderPort;

class PublicKeyResponseTest {

    @Test
    void shouldConstructRecordWithAllFields() {
        var response = new PublicKeyResponse("-----BEGIN PUBLIC KEY-----", "kid-1");
        assertEquals("-----BEGIN PUBLIC KEY-----", response.publicKey());
        assertEquals("kid-1", response.kid());
    }

    @Test
    void fromShouldMapAllFieldsFromPublicKeyInfo() {
        var info = new CryptoKeyProviderPort.PublicKeyInfo("pem-data", "key-123");
        var response = PublicKeyResponse.from(info);
        assertEquals("pem-data", response.publicKey());
        assertEquals("key-123", response.kid());
    }

    @Test
    void fromShouldHandleNullFieldsInInfo() {
        var info = new CryptoKeyProviderPort.PublicKeyInfo(null, null);
        var response = PublicKeyResponse.from(info);
        assertNull(response.publicKey());
        assertNull(response.kid());
    }

    @Test
    void shouldHaveValueEquality() {
        var a = new PublicKeyResponse("pem", "kid");
        var b = new PublicKeyResponse("pem", "kid");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenFieldsDiffer() {
        var a = new PublicKeyResponse("pem1", "kid");
        var b = new PublicKeyResponse("pem2", "kid");
        assertNotEquals(a, b);
    }
}
