package trazzo.back.saasglobal.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EncryptionServiceTest {

    private EncryptionService service;

    @BeforeEach
    void setUp() {
        service = new EncryptionService("AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQE=");
    }

    @Test
    void encryptAndDecrypt_roundtrip() {
        String plaintext = "super-secret-password-123";

        String encrypted = service.encrypt(plaintext);
        String decrypted = service.decrypt(encrypted);

        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    void encrypt_producesUniqueOutputsForSamePlaintext() {
        String first = service.encrypt("password");
        String second = service.encrypt("password");

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void encrypt_returnsValidBase64() {
        String encrypted = service.encrypt("test");

        assertThat(Base64.getDecoder().decode(encrypted)).isNotEmpty();
    }

    @Test
    void decrypt_throwsOnDataTooShort() {
        assertThatThrownBy(() -> service.decrypt("AAAA"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Decryption failed");
    }

    @Test
    void constructor_throwsWhenKeyIsNot32Bytes() {
        assertThatThrownBy(() -> new EncryptionService("dG9vc2hvcnQ="))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("256 bits");
    }
}
