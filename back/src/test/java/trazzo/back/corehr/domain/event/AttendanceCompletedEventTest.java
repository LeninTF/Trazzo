package trazzo.back.corehr.domain.event;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import trazzo.back.corehr.domain.model.AttendanceState;
import java.time.LocalDateTime;

class AttendanceCompletedEventTest {

    @Test
    void shouldCreateAttendanceCompletedEvent() {
        var now = LocalDateTime.now();
        var event = new AttendanceCompletedEvent("att-1", 1L, now, AttendanceState.PUNTUAL, now);
        assertEquals("att-1", event.attendanceId());
        assertEquals(AttendanceState.PUNTUAL, event.state());
        assertTrue(event instanceof CoreHrDomainEvent);
    }
}
