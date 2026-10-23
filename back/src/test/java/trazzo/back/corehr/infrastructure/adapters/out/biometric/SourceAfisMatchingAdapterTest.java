package trazzo.back.corehr.infrastructure.adapters.out.biometric;

import org.junit.jupiter.api.Test;
import trazzo.back.corehr.application.port.out.BiometricCryptoPort;
import trazzo.back.corehr.domain.exception.BiometricDecryptionException;
import trazzo.back.corehr.domain.model.attendance.UserBiometria;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Usa templates SourceAFIS reales generados por el middleware .NET (ver resources/biometric/README.md):
 * la coincidencia la decide el motor, no un mock. El cifrado se sustituye por un abridor de prueba que
 * decodifica el Base64 guardado, o falla si el valor guardado es "corrupto".
 */
class SourceAfisMatchingAdapterTest {

    private static final LocalDateTime NOW = LocalDateTime.now();

    private static final BiometricCryptoPort FAKE_OPENER = payload -> {
        if (payload.encryptedTemplateBase64().equals("corrupto")) {
            throw new BiometricDecryptionException("Error descifrado biométrico: tag", null);
        }
        return Base64.getDecoder().decode(payload.encryptedTemplateBase64());
    };

    private final SourceAfisMatchingAdapter adapter = new SourceAfisMatchingAdapter(FAKE_OPENER);

    private static byte[] template(String name) throws IOException {
        try (InputStream in = SourceAfisMatchingAdapterTest.class.getResourceAsStream("/biometric/" + name)) {
            return Base64.getDecoder().decode(new String(in.readAllBytes(), StandardCharsets.US_ASCII).trim());
        }
    }

    private static UserBiometria enrolled(long tenantUserId, String storedTemplate) {
        return UserBiometria.restore(tenantUserId, tenantUserId, 1L, "DVC-001", 1,
                storedTemplate, "llave", "iv", "tag", NOW, true, NOW, NOW);
    }

    private static UserBiometria enrolled(long tenantUserId, byte[] plainTemplate) {
        return enrolled(tenantUserId, Base64.getEncoder().encodeToString(plainTemplate));
    }

    @Test
    void identifiesTheOwnerOfTheMatchingTemplate() throws IOException {
        var result = adapter.identify(template("sourceafis-template-a.b64"),
                List.of(enrolled(200L, template("sourceafis-template-b.b64")), enrolled(100L, template("sourceafis-template-a.b64"))),
                40);

        assertThat(result).isPresent();
        assertThat(result.get().matched()).isTrue();
        assertThat(result.get().tenantUserId()).isEqualTo(100L);
        assertThat(result.get().confidence()).isGreaterThanOrEqualTo(80);
    }

    @Test
    void returnsEmptyWhenNoEnrolledTemplateReachesTheThreshold() throws IOException {
        var result = adapter.identify(template("sourceafis-template-a.b64"),
                List.of(enrolled(200L, template("sourceafis-template-b.b64"))), 40);

        assertThat(result).isEmpty();
    }

    @Test
    void returnsEmptyWhenTheBestScoreIsBelowAHighThreshold() throws IOException {
        var result = adapter.identify(template("sourceafis-template-a.b64"),
                List.of(enrolled(100L, template("sourceafis-template-a.b64"))), 500);

        assertThat(result).isEmpty();
    }

    @Test
    void skipsTemplatesThatCannotBeOpenedAndStillMatchTheRest() throws IOException {
        var result = adapter.identify(template("sourceafis-template-a.b64"),
                List.of(enrolled(300L, "corrupto"), enrolled(100L, template("sourceafis-template-a.b64"))), 40);

        assertThat(result).isPresent();
        assertThat(result.get().tenantUserId()).isEqualTo(100L);
    }

    @Test
    void returnsEmptyWhenEveryTemplateFailsToOpen() throws IOException {
        var result = adapter.identify(template("sourceafis-template-a.b64"),
                List.of(enrolled(300L, "corrupto"), enrolled(301L, "corrupto")), 40);

        assertThat(result).isEmpty();
    }

    @Test
    void returnsEmptyForAProbeThatIsNotASourceAfisTemplate() throws IOException {
        var result = adapter.identify(new byte[]{1, 2, 3},
                List.of(enrolled(100L, template("sourceafis-template-a.b64"))), 40);

        assertThat(result).isEmpty();
    }

    @Test
    void returnsEmptyWithoutEnrolledTemplates() throws IOException {
        assertThat(adapter.identify(template("sourceafis-template-a.b64"), List.of(), 40)).isEmpty();
        assertThat(adapter.identify(template("sourceafis-template-a.b64"), null, 40)).isEmpty();
    }
}
