package trazzo.back.incidents.application.port.out;

import trazzo.back.incidents.domain.event.IncidentDomainEvent;

public interface EventPublisherPort {
    void publish(IncidentDomainEvent event);
}
