package trazzo.back.organization.domain.exception;

public class OrgValidationException extends RuntimeException {
    public OrgValidationException(String message) {
        super(message);
    }
}
