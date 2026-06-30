package trazzo.back.audit.domain.exception;

public class AuditNotFoundException extends RuntimeException {
    public AuditNotFoundException(String message) {
        super(message);
    }
}
