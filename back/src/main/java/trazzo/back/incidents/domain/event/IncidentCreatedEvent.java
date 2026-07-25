package trazzo.back.incidents.domain.event;

import java.time.LocalDateTime;

public record IncidentCreatedEvent(
        Integer incidentId,
        Integer tenantUserId,
        Integer incidentTypeId,
        LocalDateTime occurredAt
) implements IncidentDomainEvent {
}
