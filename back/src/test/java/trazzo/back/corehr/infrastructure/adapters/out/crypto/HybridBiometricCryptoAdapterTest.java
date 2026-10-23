package trazzo.back.corehr.infrastructure.adapters.out.crypto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import trazzo.back.corehr.application.port.out.BiometricCryptoPort.StoredTemplate;
import trazzo.back.corehr.application.port.out.BiometricCryptoPort.TransportPayload;
import trazzo.back.corehr.application.port.out.CryptoKeyProviderPort;
import trazzo.back.corehr.domain.exception.BiometricDecryptionException;
import trazzo.back.corehr.domain.model.attendance.UserBiometria;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * El vector lo generó HybridCryptographyService del middleware .NET 10 (RSA OaepSHA256 → MGF1-SHA256,
 * AES-256-GCM con AAD "biometric-identify") cifrando un template SourceAFIS real. Si el backend vuelve a
 * usar los parámetros OAEP por defecto de SunJCE (MGF1-SHA1), este test falla con BadPadding.
 */
class HybridBiometricCryptoAdapterTest {

    private JsonNode vector;
    private PrivateKey vectorKey;

    @BeforeEach
    void loadVector() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/biometric/dotnet-interop-vector.json")) {
            vector = new ObjectMapper().readTree(in);
        }
        byte[] pkcs8 = Base64.getDecoder().decode(vector.get("test_only_private_key_pkcs8_base64").asText());
        vectorKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(pkcs8));
    }

    private TransportPayload vectorPayload(String tagBase64) {
        return new TransportPayload(
                vector.get("encrypted_template_base64").asText(),
                vector.get("encrypted_aes_key_base64").asText(),
                vector.get("iv_base64").asText(),
                tagBase64);
    }

    // 32 bytes de 0x02, solo para tests.
    private static final String STORAGE_KEY = "AgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgI=";

    private static HybridBiometricCryptoAdapter adapterWith(List<PrivateKey> keys) {
        return adapterWith(keys, STORAGE_KEY);
    }

    private static HybridBiometricCryptoAdapter adapterWith(List<PrivateKey> keys, String storageKey) {
        return adapterWith(keys, storageKey, "");
    }

    private static HybridBiometricCryptoAdapter adapterWith(List<PrivateKey> keys, String storageKey, String encryptionKey) {
        CryptoKeyProviderPort provider = new CryptoKeyProviderPort() {
            @Override public PublicKeyInfo getCurrentPublicKey() { return new PublicKeyInfo("unused", "unused"); }
            @Override public PrivateKey getPrivateKey() { return keys.getFirst(); }
            @Override public String getCurrentKid() { return "unused"; }
            @Override public List<PrivateKey> getDecryptionKeys() { return keys; }
        };
        return new HybridBiometricCryptoAdapter(provider, storageKey, encryptionKey);
    }

    // Una fila sellada no lleva llave AES envuelta: la clave de almacenamiento la identifica keyRef.
    private static UserBiometria stored(StoredTemplate sealed) {
        LocalDateTime now = LocalDateTime.now();
        return UserBiometria.restore(1L, 100L, 1L, "DEV-01", 1, sealed.ciphertextBase64(), null,
                sealed.ivBase64(), sealed.tagBase64(), now, true, now, now, sealed.keyRef());
    }

    @Test
    void aStoredTemplateOpensWithTheSameStorageKeyAfterARestart() throws Exception {
        byte[] template = Base64.getDecoder().decode(vector.get("plaintext_template_base64").asText());

        StoredTemplate sealed = adapterWith(List.of(vectorKey)).seal(template);
        byte[] reopened = adapterWith(List.of(vectorKey)).open(stored(sealed));

        assertThat(sealed.keyRef()).isEqualTo("storage-v1");
        assertThat(Base64.getDecoder().decode(sealed.ciphertextBase64())).isNotEqualTo(template);
        assertThat(reopened).isEqualTo(template);
    }

    @Test
    void aStoredTemplateWhoseCiphertextWasTamperedDoesNotOpen() {
        StoredTemplate sealed = adapterWith(List.of(vectorKey)).seal(new byte[]{1, 2, 3, 4});
        byte[] ciphertext = Base64.getDecoder().decode(sealed.ciphertextBase64());
        ciphertext[0] ^= 0x01;
        StoredTemplate tampered = new StoredTemplate(Base64.getEncoder().encodeToString(ciphertext),
                sealed.ivBase64(), sealed.tagBase64(), sealed.keyRef());

        assertThatThrownBy(() -> adapterWith(List.of(vectorKey)).open(stored(tampered)))
                .isInstanceOf(BiometricDecryptionException.class);
    }

    @Test
    void aLegacyRowWithoutKeyRefIsOpenedAsTheTransportPayloadItWasSavedAs() {
        LocalDateTime now = LocalDateTime.now();
        UserBiometria legacy = UserBiometria.restore(1L, 100L, 1L, "DEV-01", 1,
                vector.get("encrypted_template_base64").asText(), vector.get("encrypted_aes_key_base64").asText(),
                vector.get("iv_base64").asText(), vector.get("tag_base64").asText(), now, true, now, now);

        byte[] template = adapterWith(List.of(vectorKey)).open(legacy);

        assertThat(template).isEqualTo(Base64.getDecoder().decode(vector.get("plaintext_template_base64").asText()));
    }

    @Test
    void sealingWithoutAStorageKeyFailsNamingTheProperty() {
        assertThatThrownBy(() -> adapterWith(List.of(vectorKey), "").seal(new byte[]{1}))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.biometric.storage-key");
    }

    // .env con "APP_BIOMETRIC_STORAGE_KEY=" deja la propiedad vacía (no ausente): el fallback va en código.
    @Test
    void aBlankStorageKeyFallsBackToTheEncryptionKey() {
        StoredTemplate sealed = adapterWith(List.of(vectorKey), "", STORAGE_KEY).seal(new byte[]{5, 6, 7});

        assertThat(adapterWith(List.of(vectorKey), STORAGE_KEY, "").open(stored(sealed))).containsExactly(5, 6, 7);
    }

    @Test
    void aRowSealedWithAnotherStorageKeyDoesNotOpen() {
        StoredTemplate sealed = adapterWith(List.of(vectorKey)).seal(new byte[]{5, 6, 7});
        String otherKey = Base64.getEncoder().encodeToString(new byte[32]);

        assertThatThrownBy(() -> adapterWith(List.of(vectorKey), otherKey).open(stored(sealed)))
                .isInstanceOf(BiometricDecryptionException.class);
    }

    @Test
    void aStorageKeyThatIsNot32BytesFailsAtStartup() {
        assertThatThrownBy(() -> adapterWith(List.of(vectorKey), Base64.getEncoder().encodeToString(new byte[16])))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.biometric.storage-key");
    }

    @Test
    void opensTheTransportPayloadProducedByTheDotnetMiddleware() {
        byte[] template = adapterWith(List.of(vectorKey)).openTransport(vectorPayload(vector.get("tag_base64").asText()));

        assertThat(template).isEqualTo(Base64.getDecoder().decode(vector.get("plaintext_template_base64").asText()));
    }

    @Test
    void triesEveryDecryptionKeyInOrder() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        PrivateKey unrelated = generator.generateKeyPair().getPrivate();

        byte[] template = adapterWith(List.of(unrelated, vectorKey)).openTransport(vectorPayload(vector.get("tag_base64").asText()));

        assertThat(template).hasSize(181);
    }

    @Test
    void rejectsAPayloadWhoseGcmTagWasTampered() {
        byte[] tag = Base64.getDecoder().decode(vector.get("tag_base64").asText());
        tag[0] ^= 0x01;

        assertThatThrownBy(() -> adapterWith(List.of(vectorKey)).openTransport(vectorPayload(Base64.getEncoder().encodeToString(tag))))
                .isInstanceOf(BiometricDecryptionException.class)
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMalformedBase64() {
        TransportPayload payload = new TransportPayload("%%%", "%%%", "%%%", "%%%");

        assertThatThrownBy(() -> adapterWith(List.of(vectorKey)).openTransport(payload))
                .isInstanceOf(BiometricDecryptionException.class);
    }
}
