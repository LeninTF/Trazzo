package trazzo.back.saasglobal.domain.exception;

public class InvalidTenantTransitionException extends RuntimeException {
    public InvalidTenantTransitionException(String message) {
        super(message);
    }
}
