package trazzo.back.saasglobal.domain.exception;

public class RequestRateLimitException extends RuntimeException {
    public RequestRateLimitException(String message) {
        super(message);
    }
}
