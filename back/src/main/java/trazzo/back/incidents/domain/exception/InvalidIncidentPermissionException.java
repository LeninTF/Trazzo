package trazzo.back.incidents.domain.exception;

public class InvalidIncidentPermissionException extends IllegalArgumentException {
    public InvalidIncidentPermissionException(String message) {
        super(message);
    }
}
