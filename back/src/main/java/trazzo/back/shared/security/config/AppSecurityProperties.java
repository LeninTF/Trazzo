package trazzo.back.shared.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security")
public record AppSecurityProperties(
        int maxFailedAttempts,
        int lockoutDurationMinutes
) {}
