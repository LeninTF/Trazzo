package trazzo.back.corehr.domain.exception;

public class InvalidScheduleException extends IllegalArgumentException {
    public InvalidScheduleException(String message) {
        super(message);
    }
}
