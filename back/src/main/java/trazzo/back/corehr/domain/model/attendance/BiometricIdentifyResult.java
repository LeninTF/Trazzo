package trazzo.back.corehr.domain.model.attendance;

public record BiometricIdentifyResult(boolean matched, Long tenantUserId, int confidence) {
    public static BiometricIdentifyResult noMatch() {
        return new BiometricIdentifyResult(false, null, 0);
    }

    public static BiometricIdentifyResult match(Long tenantUserId, int confidence) {
        return new BiometricIdentifyResult(true, tenantUserId, confidence);
    }
}
