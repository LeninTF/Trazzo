package trazzo.back.audit.infrastructure.adapters.in.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import trazzo.back.audit.domain.model.master.LogInHistory;
import trazzo.back.audit.infrastructure.adapters.out.persistence.repository.LogInHistoryRepository;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class LoginAuditListener {

    private final LogInHistoryRepository repository;

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        repository.save(LogInHistory.builder()
                .email(event.getAuthentication().getName())
                .ipAddress(getClientIp())
                .userAgent(getUserAgent())
                .success(true)
                .timestamp(Instant.now())
                .build());
    }

    @EventListener
    public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        repository.save(LogInHistory.builder()
                .email(event.getAuthentication().getName())
                .ipAddress(getClientIp())
                .userAgent(getUserAgent())
                .success(false)
                .failureReason(event.getException().getMessage())
                .timestamp(Instant.now())
                .build());
    }

    private String getClientIp() {
        try {
            HttpServletRequest request = ((ServletRequestAttributes)
                    RequestContextHolder.currentRequestAttributes()).getRequest();
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            return xForwardedFor != null ? xForwardedFor.split(",")[0].trim() : request.getRemoteAddr();
        } catch (IllegalStateException e) {
            return "unknown";
        }
    }

    private String getUserAgent() {
        try {
            HttpServletRequest request = ((ServletRequestAttributes)
                    RequestContextHolder.currentRequestAttributes()).getRequest();
            return request.getHeader("User-Agent");
        } catch (IllegalStateException e) {
            return "unknown";
        }
    }
}
