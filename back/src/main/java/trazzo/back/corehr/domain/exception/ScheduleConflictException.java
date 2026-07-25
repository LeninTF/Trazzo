package trazzo.back.corehr.domain.exception;

public class ScheduleConflictException extends IllegalStateException {
    public ScheduleConflictException(String message) {
        super(message);
    }
}
