package trazzo.back.incidents.infrastructure.adapters.out.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import trazzo.back.incidents.application.port.out.EventPublisherPort;
import trazzo.back.incidents.domain.event.IncidentDomainEvent;

@Component
@RequiredArgsConstructor
public class SpringEventPublisherAdapter implements EventPublisherPort {

    private final ApplicationEventPublisher springPublisher;

    @Override
    public void publish(IncidentDomainEvent event) {
        springPublisher.publishEvent(event);
    }
}
