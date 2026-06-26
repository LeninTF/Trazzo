package trazzo.back.saasglobal.domain.exception;

public class TenantAlreadyActivatedException extends RuntimeException {
    public TenantAlreadyActivatedException(String message) {
        super(message);
    }
}
