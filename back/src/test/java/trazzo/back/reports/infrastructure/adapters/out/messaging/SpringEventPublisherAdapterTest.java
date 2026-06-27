package trazzo.back.reports.infrastructure.adapters.out.messaging;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import trazzo.back.reports.domain.event.DomainEvent;
import trazzo.back.reports.domain.event.MonthlyClosureCreatedEvent;
import trazzo.back.reports.domain.model.closure.ClosurePeriod;

import java.time.LocalDateTime;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class SpringEventPublisherAdapterTest {

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private SpringEventPublisherAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new SpringEventPublisherAdapter(applicationEventPublisher);
    }

    @Test
    void shouldPublishDomainEvent() {
        DomainEvent event = new MonthlyClosureCreatedEvent(
                UUID.randomUUID(), new ClosurePeriod(6, 2025), UUID.randomUUID(), LocalDateTime.now());

        adapter.publishEvent(event);

        verify(applicationEventPublisher).publishEvent(event);
    }
}
