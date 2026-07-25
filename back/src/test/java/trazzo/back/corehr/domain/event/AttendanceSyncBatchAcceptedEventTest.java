package trazzo.back.corehr.domain.event;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.UUID;

class AttendanceSyncBatchAcceptedEventTest {

    @Test
    void shouldCreateAttendanceSyncBatchAcceptedEvent() {
        var correlationId = UUID.randomUUID();
        var now = LocalDateTime.now();
        var event = new AttendanceSyncBatchAcceptedEvent(correlationId, 5, 10L, now);
        assertEquals(correlationId, event.correlationId());
        assertEquals(5, event.acceptedCount());
        assertEquals(10L, event.tenantUserId());
        assertEquals(now, event.occurredAt());
    }

    @Test
    void shouldImplementCoreHrDomainEvent() {
        var event = new AttendanceSyncBatchAcceptedEvent(UUID.randomUUID(), 1, 1L, LocalDateTime.now());
        assertTrue(event instanceof CoreHrDomainEvent);
    }

    @Test
    void shouldHaveValueEquality() {
        var id = UUID.randomUUID();
        var now = LocalDateTime.of(2026, 7, 19, 10, 0);
        var a = new AttendanceSyncBatchAcceptedEvent(id, 3, 1L, now);
        var b = new AttendanceSyncBatchAcceptedEvent(id, 3, 1L, now);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenFieldsDiffer() {
        var now = LocalDateTime.now();
        var a = new AttendanceSyncBatchAcceptedEvent(UUID.randomUUID(), 5, 1L, now);
        var b = new AttendanceSyncBatchAcceptedEvent(UUID.randomUUID(), 5, 1L, now);
        assertNotEquals(a, b);
    }
}
