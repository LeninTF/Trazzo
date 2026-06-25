package trazzo.back.incidents.domain.event;

import java.time.LocalDateTime;

public record IncidentCreatedEvent(
        String incidentId,
        String tenantUserId,
        String incidentTypeId,
        LocalDateTime occurredAt
) implements IncidentDomainEvent {
}
