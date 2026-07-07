package trazzo.back.organization.domain.exception;

public class DuplicateOrgNameException extends RuntimeException {
    public DuplicateOrgNameException(String message) {
        super(message);
    }
}
