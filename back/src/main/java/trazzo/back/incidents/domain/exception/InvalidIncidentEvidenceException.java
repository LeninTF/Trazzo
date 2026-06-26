package trazzo.back.incidents.domain.exception;

public class InvalidIncidentEvidenceException extends IllegalArgumentException {
    public InvalidIncidentEvidenceException(String message) {
        super(message);
    }
}
