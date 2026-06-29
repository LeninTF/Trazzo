package trazzo.back.saasglobal.infrastructure.adapters.out.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import trazzo.back.saasglobal.application.port.out.EventPublisherPort;
import trazzo.back.saasglobal.domain.event.SaasGlobalDomainEvent;

@Component
@RequiredArgsConstructor
public class SpringEventPublisherAdapter implements EventPublisherPort {

    private final ApplicationEventPublisher publisher;

    @Override
    public void publish(SaasGlobalDomainEvent event) {
        publisher.publishEvent(event);
    }
}
