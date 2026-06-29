package trazzo.back.corehr.infrastructure.adapters.out.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import trazzo.back.corehr.application.port.out.EventPublisherPort;
import trazzo.back.corehr.domain.event.CoreHrDomainEvent;

@Component("corehrEventPublisherAdapter")
@RequiredArgsConstructor
public class SpringEventPublisherAdapter implements EventPublisherPort {

    private final ApplicationEventPublisher springPublisher;

    @Override
    public void publish(CoreHrDomainEvent event) {
        springPublisher.publishEvent(event);
    }
}
