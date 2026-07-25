package trazzo.back.incidents.domain.event;

import java.time.LocalDateTime;

public record IncidentEvidenceRegisteredEvent(
        Integer incidentId,
        Integer evidenceId,
        String fileName,
        String fileKey,
        LocalDateTime occurredAt
) implements IncidentDomainEvent {
}
