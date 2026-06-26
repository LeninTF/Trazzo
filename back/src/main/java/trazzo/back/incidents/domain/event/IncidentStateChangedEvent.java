package trazzo.back.incidents.domain.event;

import java.time.LocalDateTime;
import trazzo.back.incidents.domain.model.IncidentState;

public record IncidentStateChangedEvent(
        String incidentId,
        String tenantUserId,
        IncidentState previousState,
        IncidentState newState,
        String rejectionReason,
        LocalDateTime occurredAt
) implements IncidentDomainEvent {
}
