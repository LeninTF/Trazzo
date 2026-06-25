package trazzo.back.incidents.domain.exception;

public class IncidentValidationException extends IllegalArgumentException {
    public IncidentValidationException(String message) {
        super(message);
    }
}
