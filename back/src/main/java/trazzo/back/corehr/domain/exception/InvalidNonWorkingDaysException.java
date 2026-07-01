package trazzo.back.corehr.domain.exception;

public class InvalidNonWorkingDaysException extends IllegalArgumentException {
    public InvalidNonWorkingDaysException(String message) {
        super(message);
    }
}
