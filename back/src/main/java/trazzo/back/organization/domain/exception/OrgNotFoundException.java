package trazzo.back.organization.domain.exception;

public class OrgNotFoundException extends RuntimeException {
    public OrgNotFoundException(String message) {
        super(message);
    }
}
