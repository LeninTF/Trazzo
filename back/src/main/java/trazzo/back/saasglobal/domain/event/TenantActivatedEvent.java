package trazzo.back.saasglobal.domain.event;

import java.time.LocalDateTime;

public record TenantActivatedEvent(
        String tenantId,
        String subDomain,
        LocalDateTime occurredAt
) implements SaasGlobalDomainEvent {
}
