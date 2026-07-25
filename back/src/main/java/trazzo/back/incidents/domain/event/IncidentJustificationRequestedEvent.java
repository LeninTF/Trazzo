package trazzo.back.incidents.domain.event;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record IncidentJustificationRequestedEvent(
        Integer incidentId,
        Integer tenantUserId,
        LocalDate startDate,
        LocalDate endDate,
        LocalDateTime occurredAt
) implements IncidentDomainEvent {
}
