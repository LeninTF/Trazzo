package trazzo.back.corehr.application.port.out;

import trazzo.back.corehr.domain.event.CoreHrDomainEvent;

public interface EventPublisherPort {
    void publish(CoreHrDomainEvent event);
}
