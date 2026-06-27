package trazzo.back.reports.domain.event;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import trazzo.back.reports.domain.model.closure.ClosurePeriod;

import java.time.LocalDateTime;
import java.util.UUID;

class MonthlyClosureCreatedEventTest {

    @Test
    void shouldCreateEventSuccessfully() {
        UUID closureId = UUID.randomUUID();
        ClosurePeriod period = new ClosurePeriod(6, 2025);
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        MonthlyClosureCreatedEvent event = new MonthlyClosureCreatedEvent(closureId, period, userId, now);

        assertEquals(closureId, event.getClosureId());
        assertEquals(period, event.getPeriod());
        assertEquals(userId, event.getCreatedByUserId());
        assertEquals(now, event.getCreatedAt());
    }

    @Test
    void shouldThrowExceptionWhenClosureIdIsNull() {
        assertThrows(NullPointerException.class,
                () -> new MonthlyClosureCreatedEvent(null, new ClosurePeriod(6, 2025), UUID.randomUUID(), LocalDateTime.now()));
    }

    @Test
    void shouldThrowExceptionWhenPeriodIsNull() {
        assertThrows(NullPointerException.class,
                () -> new MonthlyClosureCreatedEvent(UUID.randomUUID(), null, UUID.randomUUID(), LocalDateTime.now()));
    }

    @Test
    void shouldThrowExceptionWhenCreatedByUserIdIsNull() {
        assertThrows(NullPointerException.class,
                () -> new MonthlyClosureCreatedEvent(UUID.randomUUID(), new ClosurePeriod(6, 2025), null, LocalDateTime.now()));
    }

    @Test
    void shouldThrowExceptionWhenCreatedAtIsNull() {
        assertThrows(NullPointerException.class,
                () -> new MonthlyClosureCreatedEvent(UUID.randomUUID(), new ClosurePeriod(6, 2025), UUID.randomUUID(), null));
    }
}
