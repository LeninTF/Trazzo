package trazzo.back.incidents.domain.event;

import java.time.LocalDateTime;

public interface IncidentDomainEvent {
    LocalDateTime occurredAt();
}
