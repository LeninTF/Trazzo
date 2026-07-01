package trazzo.back.reports.infrastructure.adapters.out.messaging;

import org.springframework.context.ApplicationEventPublisher;
import trazzo.back.reports.application.ports.out.EventPublisherPort;
import trazzo.back.reports.domain.event.DomainEvent;

public class SpringEventPublisherAdapter implements EventPublisherPort {

    private final ApplicationEventPublisher applicationEventPublisher;

    public SpringEventPublisherAdapter(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publishEvent(DomainEvent event) {
        applicationEventPublisher.publishEvent(event);
    }
}
