package trazzo.back.corehr.infrastructure.adapters.out.biometric;

import com.machinezoo.sourceafis.FingerprintImage;
import com.machinezoo.sourceafis.FingerprintTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import trazzo.back.corehr.application.port.out.CryptoKeyProviderPort;
import trazzo.back.corehr.domain.model.attendance.BiometricIdentifyResult;
import trazzo.back.corehr.domain.model.attendance.UserBiometria;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SourceAfisMatchingAdapterTest {

    @Mock
    CryptoKeyProviderPort cryptoKeyProvider;

    @InjectMocks
    SourceAfisMatchingAdapter adapter;

    private static final LocalDateTime NOW = LocalDateTime.now();
    private PrivateKey privateKey;
    private PublicKey publicKey;
    private SecretKey aesKey;
    private byte[] templateBytes;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair keyPair = kpg.generateKeyPair();
        privateKey = keyPair.getPrivate();
        publicKey = keyPair.getPublic();

        javax.crypto.KeyGenerator kg = javax.crypto.KeyGenerator.getInstance("AES");
        kg.init(256);
        aesKey = kg.generateKey();

        templateBytes = createSyntheticTemplate();
    }

    private byte[] createSyntheticTemplate() throws Exception {
        int width = 288;
        int height = 384;
        byte[] pixels = new byte[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixels[y * width + x] = (byte) ((x * 7 + y * 3) % 256);
            }
        }
        return new FingerprintTemplate(new FingerprintImage(width, height, pixels)).toByteArray();
    }

    private UserBiometria createEnrolledBiometria(byte[] plainTemplate) throws Exception {
        byte[] iv = new byte[12];
        new java.security.SecureRandom().nextBytes(iv);

        Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
        aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec);
        aesCipher.updateAAD("biometric-identify".getBytes());
        byte[] encrypted = aesCipher.doFinal(plainTemplate);
        byte[] ciphertext = new byte[encrypted.length - 16];
        byte[] tag = new byte[16];
        System.arraycopy(encrypted, 0, ciphertext, 0, ciphertext.length);
        System.arraycopy(encrypted, ciphertext.length, tag, 0, 16);

        Cipher rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedAesKey = rsaCipher.doFinal(aesKey.getEncoded());

        return UserBiometria.restore(
                1L, 100L, 1L, "DVC-001", 1,
                Base64.getEncoder().encodeToString(ciphertext),
                Base64.getEncoder().encodeToString(encryptedAesKey),
                Base64.getEncoder().encodeToString(iv),
                Base64.getEncoder().encodeToString(tag),
                NOW, true, NOW, NOW
        );
    }

    private UserBiometria anEnrolledTemplate() {
        return UserBiometria.restore(
                1L, 100L, 1L, "DVC-001", 1,
                "dGVtcA==", "a2V5", "aXY=", "dGFn",
                NOW, true, NOW, NOW
        );
    }

    @Test
    void identify_withEmptyEnrolledTemplates_shouldReturnEmpty() {
        var result = adapter.identify(templateBytes, List.of(), 40);
        assertThat(result).isEmpty();
    }

    @Test
    void identify_withGarbageProbeTemplate_shouldReturnEmpty() {
        byte[] probe = "garbage-probe-data".getBytes();
        var result = adapter.identify(probe, List.of(anEnrolledTemplate()), 40);
        assertThat(result).isEmpty();
    }

    @Test
    void identify_withAllDecryptionFailures_shouldReturnEmpty() {
        byte[] probe = "garbage-probe-data".getBytes();
        var result = adapter.identify(probe, List.of(anEnrolledTemplate()), 40);
        assertThat(result).isEmpty();
    }

    @Test
    void identify_withMultipleEnrolledTemplatesAndGarbageProbe_shouldReturnEmpty() {
        var template1 = UserBiometria.restore(
                1L, 100L, 1L, "DVC-001", 1,
                "dGVtcA==", "a2V5", "aXY=", "dGFn",
                NOW, true, NOW, NOW
        );
        var template2 = UserBiometria.restore(
                2L, 200L, 1L, "DVC-001", 2,
                "dGVtcA==", "a2V5", "aXY=", "dGFn",
                NOW, true, NOW, NOW
        );
        byte[] probe = "garbage-probe-data".getBytes();

        var result = adapter.identify(probe, List.of(template1, template2), 40);

        assertThat(result).isEmpty();
    }

    @Test
    void identify_withEmptyByteArrayProbe_shouldReturnEmpty() {
        var result = adapter.identify(new byte[0], List.of(), 40);
        assertThat(result).isEmpty();
    }

    @Test
    void identify_withNullEnrolledTemplates_shouldReturnEmpty() {
        byte[] probe = "garbage-probe-data".getBytes();
        var result = adapter.identify(probe, List.of(), 40);
        assertThat(result).isEmpty();
    }

    @Test
    void identify_withValidProbeAndDecryptionFailure_shouldReturnEmpty() throws Exception {
        when(cryptoKeyProvider.getPrivateKey()).thenReturn(privateKey);

        var result = adapter.identify(templateBytes, List.of(anEnrolledTemplate()), 40);

        assertThat(result).isEmpty();
    }

    @Test
    void identify_withValidProbeAndMultipleDecryptionFailures_shouldReturnEmpty() throws Exception {
        when(cryptoKeyProvider.getPrivateKey()).thenReturn(privateKey);

        var bad1 = UserBiometria.restore(1L, 100L, 1L, "DVC-001", 1, "bad", "bad", "bad", "bad", NOW, true, NOW, NOW);
        var bad2 = UserBiometria.restore(2L, 200L, 1L, "DVC-001", 2, "bad", "bad", "bad", "bad", NOW, true, NOW, NOW);

        var result = adapter.identify(templateBytes, List.of(bad1, bad2), 40);

        assertThat(result).isEmpty();
    }

    @Test
    void identify_withValidEnrolledTemplateAndHighThreshold_shouldReturnEmpty() throws Exception {
        when(cryptoKeyProvider.getPrivateKey()).thenReturn(privateKey);
        var enrolled = createEnrolledBiometria(templateBytes);

        var result = adapter.identify(templateBytes, List.of(enrolled), 10000);

        assertThat(result).isEmpty();
    }

    @Test
    void identify_withValidEnrolledTemplateAndLowThreshold_shouldReturnMatch() throws Exception {
        when(cryptoKeyProvider.getPrivateKey()).thenReturn(privateKey);
        var enrolled = createEnrolledBiometria(templateBytes);

        var result = adapter.identify(templateBytes, List.of(enrolled), 1);

        assertThat(result).isPresent();
        assertThat(result.get().matched()).isTrue();
        assertThat(result.get().tenantUserId()).isEqualTo(100L);
        assertThat(result.get().confidence()).isGreaterThan(0);
    }

    @Test
    void identify_withValidAndInvalidEnrolled_shouldMatchValidOne() throws Exception {
        when(cryptoKeyProvider.getPrivateKey()).thenReturn(privateKey);
        var validEnrolled = createEnrolledBiometria(templateBytes);
        var invalidEnrolled = UserBiometria.restore(2L, 200L, 1L, "DVC-001", 2, "bad", "bad", "bad", "bad", NOW, true, NOW, NOW);

        var result = adapter.identify(templateBytes, List.of(invalidEnrolled, validEnrolled), 1);

        assertThat(result).isPresent();
        assertThat(result.get().tenantUserId()).isEqualTo(100L);
    }

    @Test
    void identify_withMultipleValidEnrolled_shouldReturnBestMatch() throws Exception {
        when(cryptoKeyProvider.getPrivateKey()).thenReturn(privateKey);
        var enrolled1 = createEnrolledBiometria(templateBytes);
        var enrolled2 = createEnrolledBiometria(templateBytes);

        var result = adapter.identify(templateBytes, List.of(enrolled1, enrolled2), 1);

        assertThat(result).isPresent();
        assertThat(result.get().matched()).isTrue();
    }

    @Test
    void identify_withEmptyEnrolledList_shouldReturnEmpty() {
        var result = adapter.identify(templateBytes, List.of(), 1);
        assertThat(result).isEmpty();
    }

    @Test
    void identify_withProbeCausingTemplateParseError_shouldReturnEmpty() {
        var result = adapter.identify(new byte[]{1, 2, 3}, List.of(), 40);
        assertThat(result).isEmpty();
    }
}
