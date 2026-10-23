package trazzo.back.corehr.infrastructure.adapters.out.biometric;

import com.machinezoo.sourceafis.FingerprintMatcher;
import com.machinezoo.sourceafis.FingerprintTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import trazzo.back.corehr.application.port.out.BiometricCryptoPort;
import trazzo.back.corehr.application.port.out.BiometricMatchingPort;
import trazzo.back.corehr.domain.exception.BiometricTemplateFormatException;
import trazzo.back.corehr.domain.model.attendance.BiometricIdentifyResult;
import trazzo.back.corehr.domain.model.attendance.UserBiometria;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class SourceAfisMatchingAdapter implements BiometricMatchingPort {

    private final BiometricCryptoPort biometricCrypto;

    public SourceAfisMatchingAdapter(BiometricCryptoPort biometricCrypto) {
        this.biometricCrypto = biometricCrypto;
    }

    @Override
    public Optional<BiometricIdentifyResult> identify(byte[] probeTemplate, List<UserBiometria> enrolledTemplates, int threshold) {
        if (enrolledTemplates == null || enrolledTemplates.isEmpty()) {
            return Optional.empty();
        }

        FingerprintMatcher matcher;
        try {
            // 1:N: el matcher se construye una vez a partir de la muestra y se compara con cada candidato.
            matcher = new FingerprintMatcher(new FingerprintTemplate(probeTemplate));
        } catch (RuntimeException e) {
            log.warn("La muestra biométrica no es un template SourceAFIS válido: {}", e.getMessage());
            return Optional.empty();
        }

        BiometricIdentifyResult best = null;
        double bestScore = 0;
        int unreadable = 0;
        for (UserBiometria enrolled : enrolledTemplates) {
            FingerprintTemplate candidate;
            try {
                candidate = new FingerprintTemplate(biometricCrypto.open(enrolled));
            } catch (RuntimeException e) {
                unreadable++;
                continue;
            }
            double score = matcher.match(candidate);
            if (score > bestScore) {
                bestScore = score;
                best = BiometricIdentifyResult.match(enrolled.getTenantUserId(), (int) score);
            }
        }

        if (unreadable > 0) {
            log.warn("{} de {} huellas enroladas no se pudieron abrir y se omitieron del matching",
                    unreadable, enrolledTemplates.size());
        }
        return bestScore >= threshold ? Optional.ofNullable(best) : Optional.empty();
    }

    @Override
    public void requireValidTemplate(byte[] template) {
        try {
            new FingerprintTemplate(template);
        } catch (RuntimeException e) {
            throw new BiometricTemplateFormatException("La huella recibida no es un template SourceAFIS válido", e);
        }
    }
}
