package trazzo.back.saasglobal.infrastructure.adapters.out.messaging;

import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import trazzo.back.saasglobal.domain.event.TenantActivatedEvent;

@ExtendWith(MockitoExtension.class)
class SpringEventPublisherAdapterTest {

    @Mock ApplicationEventPublisher publisher;
    @InjectMocks SpringEventPublisherAdapter adapter;

    @Test
    void publish_delegatesToApplicationEventPublisher() {
        var event = new TenantActivatedEvent("tenant-1", "sub.domain.com", LocalDateTime.now());

        adapter.publish(event);

        verify(publisher).publishEvent(event);
    }
}
