package trazzo.back.incidents.domain.exception;

public class InvalidIncidentStateException extends IllegalStateException {
    public InvalidIncidentStateException(String message) {
        super(message);
    }
}
