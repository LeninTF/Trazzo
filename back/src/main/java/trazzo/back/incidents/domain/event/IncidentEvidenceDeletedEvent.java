package trazzo.back.incidents.domain.event;

import java.time.LocalDateTime;

public record IncidentEvidenceDeletedEvent(
        String incidentId,
        String evidenceId,
        LocalDateTime occurredAt
) implements IncidentDomainEvent {
}
