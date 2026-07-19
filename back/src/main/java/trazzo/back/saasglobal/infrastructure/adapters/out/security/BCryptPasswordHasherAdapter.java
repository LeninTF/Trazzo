package trazzo.back.saasglobal.infrastructure.adapters.out.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import trazzo.back.saasglobal.application.port.out.PasswordHasherPort;

@Component
@RequiredArgsConstructor
public class BCryptPasswordHasherAdapter implements PasswordHasherPort {

    private final PasswordEncoder passwordEncoder;

    @Override
    public String hash(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
}
