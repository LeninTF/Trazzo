package trazzo.back.saasglobal.application.port.out;

import trazzo.back.saasglobal.domain.event.SaasGlobalDomainEvent;

public interface EventPublisherPort {
    void publish(SaasGlobalDomainEvent event);
}
