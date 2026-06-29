package trazzo.back.corehr.domain.exception;

public class InvalidShiftException extends IllegalArgumentException {
    public InvalidShiftException(String message) {
        super(message);
    }
}
