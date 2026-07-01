package trazzo.back.corehr.infrastructure.adapters.out.messaging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import trazzo.back.corehr.domain.event.CoreHrDomainEvent;

import java.time.LocalDateTime;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SpringEventPublisherAdapterTest {

    @Mock
    ApplicationEventPublisher springPublisher;

    @InjectMocks
    SpringEventPublisherAdapter adapter;

    private static CoreHrDomainEvent anEvent() {
        return () -> LocalDateTime.now();
    }

    @Test
    void publish_delegatesToSpringPublisher() {
        var event = anEvent();
        adapter.publish(event);
        verify(springPublisher).publishEvent(event);
    }
}
