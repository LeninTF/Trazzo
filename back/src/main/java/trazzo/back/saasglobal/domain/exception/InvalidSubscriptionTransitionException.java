package trazzo.back.saasglobal.domain.exception;

public class InvalidSubscriptionTransitionException extends RuntimeException {
    public InvalidSubscriptionTransitionException(String message) {
        super(message);
    }
}
