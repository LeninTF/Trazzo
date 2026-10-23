package trazzo.back.corehr.infrastructure.adapters.out.crypto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import trazzo.back.corehr.application.port.out.BiometricCryptoPort.TransportPayload;
import trazzo.back.corehr.application.port.out.CryptoKeyProviderPort;
import trazzo.back.corehr.domain.exception.BiometricDecryptionException;

import java.io.InputStream;
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

    private static HybridBiometricCryptoAdapter adapterWith(List<PrivateKey> keys) {
        CryptoKeyProviderPort provider = new CryptoKeyProviderPort() {
            @Override public PublicKeyInfo getCurrentPublicKey() { return new PublicKeyInfo("unused", "unused"); }
            @Override public PrivateKey getPrivateKey() { return keys.getFirst(); }
            @Override public String getCurrentKid() { return "unused"; }
            @Override public List<PrivateKey> getDecryptionKeys() { return keys; }
        };
        return new HybridBiometricCryptoAdapter(provider);
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
