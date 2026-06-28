package trazzo.back.incidents.infrastructure.adapters.out.messaging;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import trazzo.back.incidents.domain.event.IncidentCreatedEvent;

import java.time.LocalDateTime;

class SpringEventPublisherAdapterTest {

    @Test
    void publishDelegatesToSpringPublisher() {
        var springPublisher = mock(ApplicationEventPublisher.class);
        var adapter = new SpringEventPublisherAdapter(springPublisher);
        var event = new IncidentCreatedEvent("inc-1", "u-1", "t-1", LocalDateTime.now());

        adapter.publish(event);

        verify(springPublisher).publishEvent(event);
    }

    @Test
    void publishWithNullEventDoesNotThrow() {
        var springPublisher = mock(ApplicationEventPublisher.class);
        var adapter = new SpringEventPublisherAdapter(springPublisher);

        adapter.publish(null);

        verify(springPublisher).publishEvent((Object) null);
    }
}
