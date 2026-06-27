package trazzo.back.corehr.domain.exception;

public class InvalidTenantUserException extends IllegalArgumentException {
    public InvalidTenantUserException(String message) {
        super(message);
    }
}
