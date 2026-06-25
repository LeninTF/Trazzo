package trazzo.back.saasglobal.domain.event;

import java.time.LocalDateTime;

public record SubscriptionActivatedEvent(
        String subscriptionId,
        String tenantId,
        Integer planId,
        LocalDateTime occurredAt
) implements SaasGlobalDomainEvent {
}
