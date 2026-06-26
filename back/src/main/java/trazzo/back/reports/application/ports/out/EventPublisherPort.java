package trazzo.back.reports.application.ports.out;

import trazzo.back.reports.domain.event.DomainEvent;

public interface EventPublisherPort {
    void publishEvent(DomainEvent event);
}
