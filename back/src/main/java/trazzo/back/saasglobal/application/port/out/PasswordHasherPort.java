package trazzo.back.saasglobal.application.port.out;

public interface PasswordHasherPort {
    String hash(String rawPassword);
}
