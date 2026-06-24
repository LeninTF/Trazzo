package trazzo.back.saasglobal.application.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import trazzo.back.saasglobal.application.port.out.UserRepositoryPort;
import trazzo.back.shared.security.config.AppSecurityProperties;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final UserRepositoryPort userRepository;
    private final AppSecurityProperties securityProperties;

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        String email = event.getAuthentication().getName();
        userRepository.findByEmail(email).ifPresent(user -> {
            int attempts = user.getFailedAttempts() + 1;
            if (attempts >= securityProperties.maxFailedAttempts()) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(securityProperties.lockoutDurationMinutes()));
                user.setFailedAttempts(0);
            } else {
                user.setFailedAttempts(attempts);
            }
            userRepository.save(user);
        });
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        String email = event.getAuthentication().getName();
        userRepository.findByEmail(email).ifPresent(user -> {
            if (user.getFailedAttempts() > 0 || user.getLockedUntil() != null) {
                user.setFailedAttempts(0);
                user.setLockedUntil(null);
                userRepository.save(user);
            }
        });
    }
}
