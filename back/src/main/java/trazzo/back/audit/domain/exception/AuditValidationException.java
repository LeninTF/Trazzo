package trazzo.back.audit.domain.exception;

public class AuditValidationException extends RuntimeException {
    public AuditValidationException(String message) {
        super(message);
    }
}
