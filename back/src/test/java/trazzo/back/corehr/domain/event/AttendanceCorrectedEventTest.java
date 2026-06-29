package trazzo.back.corehr.domain.event;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import trazzo.back.corehr.domain.model.AttendanceState;
import java.time.LocalDateTime;

class AttendanceCorrectedEventTest {

    @Test
    void shouldCreateAttendanceCorrectedEvent() {
        var now = LocalDateTime.now();
        var event = new AttendanceCorrectedEvent(
                "att-1", 1L, AttendanceState.PUNTUAL, AttendanceState.TARDANZA, 0, 15, now
        );
        assertEquals("att-1", event.attendanceId());
        assertEquals(AttendanceState.PUNTUAL, event.previousState());
        assertEquals(AttendanceState.TARDANZA, event.newState());
        assertEquals(15, event.newMinutesLate());
        assertTrue(event instanceof CoreHrDomainEvent);
    }
}
