package trazzo.back.saasglobal.domain.exception;

public class RoleInUseException extends RuntimeException {
    public RoleInUseException(String message) {
        super(message);
    }
}
