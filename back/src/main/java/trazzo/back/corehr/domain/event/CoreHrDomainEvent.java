package trazzo.back.corehr.domain.event;

import java.time.LocalDateTime;

public interface CoreHrDomainEvent {
    LocalDateTime occurredAt();
}
