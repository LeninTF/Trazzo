package trazzo.back.corehr.application.port.out;

import java.util.Optional;
import trazzo.back.corehr.domain.model.attendance.BiometricIdentifyResult;
import trazzo.back.corehr.domain.model.attendance.UserBiometria;

import java.util.List;

public interface BiometricMatchingPort {
    Optional<BiometricIdentifyResult> identify(byte[] probeTemplate, List<UserBiometria> enrolledTemplates, int threshold);
}
