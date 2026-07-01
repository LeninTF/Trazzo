package trazzo.back.corehr.domain.exception;

public class InactiveDeviceException extends IllegalStateException {
    public InactiveDeviceException(String message) {
        super(message);
    }
}
