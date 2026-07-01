package trazzo.back.corehr.domain.event;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

class AttendanceRegisteredEventTest {

    @Test
    void shouldCreateAttendanceRegisteredEvent() {
        var now = LocalDateTime.now();
        var event = new AttendanceRegisteredEvent("att-1", 1L, 1L, 1L, now);
        assertEquals("att-1", event.attendanceId());
        assertEquals(1L, event.tenantUserId());
        assertEquals(now, event.occurredAt());
        assertTrue(event instanceof CoreHrDomainEvent);
    }
}
