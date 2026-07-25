package trazzo.back.corehr.infrastructure.adapters.out.messaging;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import trazzo.back.corehr.application.port.out.AuditScheduleAssignmentPort;

/**
 * Provides a no-op {@link AuditScheduleAssignmentPort} so the {@code UserScheduleService}
 * bean can be wired even when no concrete audit adapter is registered. The audit module
 * is allowed to override this fallback with a real adapter by exposing its own
 * {@code @Component @Primary AuditScheduleAssignmentPort} implementation.
 */
@Configuration
public class AuditScheduleAssignmentFallbackConfig {

    @Bean
    @ConditionalOnMissingBean(AuditScheduleAssignmentPort.class)
    public AuditScheduleAssignmentPort noopAuditScheduleAssignmentPort() {
        return (tenantUserId, scheduleId, userScheduleId, action) -> {
            // No-op: real implementation is provided by the audit module when available.
        };
    }
}
