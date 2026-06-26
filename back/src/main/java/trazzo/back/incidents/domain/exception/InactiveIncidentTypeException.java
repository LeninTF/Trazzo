package trazzo.back.incidents.domain.exception;

public class InactiveIncidentTypeException extends IllegalStateException {
    public InactiveIncidentTypeException(String message) {
        super(message);
    }
}
