package trazzo.back.saasglobal.domain.event;

import java.time.LocalDateTime;

public interface SaasGlobalDomainEvent {
    LocalDateTime occurredAt();
}
