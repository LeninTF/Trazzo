package trazzo.back.corehr.infrastructure.adapters.out.biometric;

import com.machinezoo.sourceafis.FingerprintMatcher;
import com.machinezoo.sourceafis.FingerprintTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import trazzo.back.corehr.application.port.out.BiometricMatchingPort;
import trazzo.back.corehr.application.port.out.CryptoKeyProviderPort;
import trazzo.back.corehr.domain.model.attendance.BiometricIdentifyResult;
import trazzo.back.corehr.domain.model.attendance.UserBiometria;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.PrivateKey;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class SourceAfisMatchingAdapter implements BiometricMatchingPort {

    private final CryptoKeyProviderPort cryptoKeyProvider;

    public SourceAfisMatchingAdapter(CryptoKeyProviderPort cryptoKeyProvider) {
        this.cryptoKeyProvider = cryptoKeyProvider;
    }

    @Override
    public Optional<BiometricIdentifyResult> identify(byte[] probeTemplate, List<UserBiometria> enrolledTemplates, int threshold) {
        try {
            FingerprintTemplate probe = new FingerprintTemplate(probeTemplate);

            BiometricIdentifyResult bestResult = BiometricIdentifyResult.noMatch();
            double bestScore = 0;

            for (UserBiometria enrolled : enrolledTemplates) {
                try {
                    byte[] enrolledBytes = decryptStoredTemplate(enrolled);
                    if (enrolledBytes == null) continue;

                    FingerprintTemplate enrolledTemplate = new FingerprintTemplate(enrolledBytes);
                    FingerprintMatcher matcher = new FingerprintMatcher(enrolledTemplate);
                    double score = matcher.match(probe);

                    if (score > bestScore) {
                        bestScore = score;
                        bestResult = BiometricIdentifyResult.match(enrolled.getTenantUserId(), (int) score);
                    }
                } catch (Exception e) {
                    log.debug("Failed to match enrolled template for user {}: {}", enrolled.getTenantUserId(), e.getMessage());
                }
            }

            if (bestScore >= threshold) {
                return Optional.of(bestResult);
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("Biometric matching error: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private byte[] decryptStoredTemplate(UserBiometria biometria) {
        try {
            PrivateKey privateKey = cryptoKeyProvider.getPrivateKey();
            byte[] encryptedAesKey = Base64.getDecoder().decode(biometria.getEncryptedAesKeyBase64());

            Cipher rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            rsaCipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] aesKeyBytes = rsaCipher.doFinal(encryptedAesKey);

            byte[] iv = Base64.getDecoder().decode(biometria.getIvBase64());
            byte[] tag = Base64.getDecoder().decode(biometria.getTagBase64());
            byte[] encryptedTemplate = Base64.getDecoder().decode(biometria.getEncryptedTemplateBase64());

            SecretKey aesKey = new SecretKeySpec(aesKeyBytes, "AES");
            Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
            aesCipher.init(Cipher.DECRYPT_MODE, aesKey, gcmSpec);

            byte[] aad = "biometric-identify".getBytes();
            aesCipher.updateAAD(aad);

            byte[] combined = new byte[encryptedTemplate.length + tag.length];
            System.arraycopy(encryptedTemplate, 0, combined, 0, encryptedTemplate.length);
            System.arraycopy(tag, 0, combined, encryptedTemplate.length, tag.length);

            return aesCipher.doFinal(combined);
        } catch (Exception e) {
            log.debug("Failed to decrypt stored template: {}", e.getMessage());
            return null;
        }
    }
}
