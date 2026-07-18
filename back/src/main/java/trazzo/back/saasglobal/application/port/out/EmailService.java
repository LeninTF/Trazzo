package trazzo.back.saasglobal.application.port.out;

public interface EmailService {
    /**
     * Best-effort send: implementations must not throw if the provider isn't configured or the
     * call fails — callers should never have their own operation fail because a notification
     * email couldn't go out.
     */
    void send(String to, String subject, String body);
}
