package trazzo.back.corehr.infrastructure.adapters.out.crypto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RsaKeyProviderAdapterTest {

    private static KeyPair newKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static String pkcs8Pem(KeyPair pair) {
        return "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(pair.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----";
    }

    // kid esperado calculado a mano: "rsa-" + 16 hex de SHA-256 del SubjectPublicKeyInfo.
    private static String expectedKid(KeyPair pair) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(pair.getPublic().getEncoded());
        return "rsa-" + HexFormat.of().formatHex(digest).substring(0, 16);
    }

    @Test
    void usesTheConfiguredPrivateKeyAndPublishesItsPublicKey() throws Exception {
        KeyPair pair = newKeyPair();

        var adapter = new RsaKeyProviderAdapter(pkcs8Pem(pair), "", "");

        assertThat(adapter.getPrivateKey().getEncoded()).isEqualTo(pair.getPrivate().getEncoded());
        String pem = adapter.getCurrentPublicKey().publicKeyPem();
        assertThat(pem).startsWith("-----BEGIN PUBLIC KEY-----").endsWith("-----END PUBLIC KEY-----");
        String body = pem.replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "").replaceAll("\\s", "");
        assertThat(Base64.getDecoder().decode(body)).isEqualTo(pair.getPublic().getEncoded());
        assertThat(adapter.getCurrentKid()).isEqualTo(expectedKid(pair));
        assertThat(adapter.getCurrentPublicKey().kid()).isEqualTo(expectedKid(pair));
    }

    @Test
    void createsTheKeyFileOnFirstStartAndReusesItAfterARestart(@TempDir Path dir) {
        Path keyFile = dir.resolve("keys").resolve("biometric-rsa.pem");

        var firstStart = new RsaKeyProviderAdapter("", keyFile.toString(), "");
        var afterRestart = new RsaKeyProviderAdapter("", keyFile.toString(), "");

        assertThat(keyFile).exists();
        assertThat(afterRestart.getCurrentKid()).isEqualTo(firstStart.getCurrentKid());
        assertThat(afterRestart.getPrivateKey().getEncoded()).isEqualTo(firstStart.getPrivateKey().getEncoded());
    }

    @Test
    void offersPreviousKeysForDecryptionAfterTheCurrentOne() throws Exception {
        KeyPair current = newKeyPair();
        KeyPair previous = newKeyPair();
        String previousBase64Der = Base64.getEncoder().encodeToString(previous.getPrivate().getEncoded());

        var adapter = new RsaKeyProviderAdapter(pkcs8Pem(current), "", previousBase64Der);

        assertThat(adapter.getDecryptionKeys()).hasSize(2);
        assertThat(adapter.getDecryptionKeys().get(0).getEncoded()).isEqualTo(current.getPrivate().getEncoded());
        assertThat(adapter.getDecryptionKeys().get(1).getEncoded()).isEqualTo(previous.getPrivate().getEncoded());
    }

    @Test
    void acceptsAPemWithEscapedNewlinesAsWrittenInAnEnvFile() throws Exception {
        KeyPair pair = newKeyPair();

        var adapter = new RsaKeyProviderAdapter(pkcs8Pem(pair).replace("\n", "\\n"), "", "");

        assertThat(adapter.getCurrentKid()).isEqualTo(expectedKid(pair));
    }

    @Test
    void anInvalidPrivateKeyFailsNamingTheProperty() {
        assertThatThrownBy(() -> new RsaKeyProviderAdapter("no-es-una-clave", "", ""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.biometric.rsa.private-key");
    }

    @Test
    void anUnreadableKeyFileFailsNamingTheProperty(@TempDir Path dir) throws Exception {
        Path keyFile = dir.resolve("biometric-rsa.pem");
        Files.writeString(keyFile, "contenido roto");

        assertThatThrownBy(() -> new RsaKeyProviderAdapter("", keyFile.toString(), ""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.biometric.rsa.key-file");
    }

    @Test
    void withoutConfigurationEachStartUsesADifferentEphemeralKey() {
        var first = new RsaKeyProviderAdapter("", "", "");
        var second = new RsaKeyProviderAdapter("", "", "");

        assertThat(first.getCurrentKid()).isNotEqualTo(second.getCurrentKid());
        assertThat(first.getDecryptionKeys()).hasSize(1);
    }
}
