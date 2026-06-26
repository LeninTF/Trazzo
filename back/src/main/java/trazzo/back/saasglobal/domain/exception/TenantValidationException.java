package trazzo.back.saasglobal.domain.exception;

public class TenantValidationException extends RuntimeException {
    public TenantValidationException(String message) {
        super(message);
    }
}
