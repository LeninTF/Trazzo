package trazzo.back.incidents.domain.event;

import java.time.LocalDateTime;

public record IncidentEvidenceDeletedEvent(
        Integer incidentId,
        Integer evidenceId,
        LocalDateTime occurredAt
) implements IncidentDomainEvent {
}
