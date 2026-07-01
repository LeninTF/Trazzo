package trazzo.back.corehr.domain.exception;

public class InvalidAttendanceException extends IllegalStateException {
    public InvalidAttendanceException(String message) {
        super(message);
    }
}
