package trazzo.back.incidents.domain.event;

import java.time.LocalDateTime;

public record IncidentEvidenceRegisteredEvent(
        String incidentId,
        String evidenceId,
        String fileName,
        String fileUrl,
        LocalDateTime occurredAt
) implements IncidentDomainEvent {
}
