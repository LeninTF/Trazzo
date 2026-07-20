package trazzo.back.corehr.domain.event;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import trazzo.back.corehr.domain.model.AttendanceState;
import java.time.LocalDateTime;

class AttendanceMarkedEventTest {

    @Test
    void shouldCreateAttendanceMarkedEvent() {
        var now = LocalDateTime.now();
        var event = new AttendanceMarkedEvent("att-1", 1L, 2L, 3L, AttendanceState.PUNTUAL, 0, now);
        assertEquals("att-1", event.attendanceId());
        assertEquals(1L, event.tenantUserId());
        assertEquals(2L, event.scheduleId());
        assertEquals(3L, event.deviceId());
        assertEquals(AttendanceState.PUNTUAL, event.state());
        assertEquals(0, event.minutesLate());
        assertEquals(now, event.occurredAt());
    }

    @Test
    void shouldImplementCoreHrDomainEvent() {
        var event = new AttendanceMarkedEvent("a", 1L, 2L, 3L, AttendanceState.TARDANZA, 15, LocalDateTime.now());
        assertTrue(event instanceof CoreHrDomainEvent);
    }

    @Test
    void shouldHaveValueEquality() {
        var now = LocalDateTime.of(2026, 7, 19, 8, 0);
        var a = new AttendanceMarkedEvent("att-1", 1L, 2L, 3L, AttendanceState.TARDANZA, 10, now);
        var b = new AttendanceMarkedEvent("att-1", 1L, 2L, 3L, AttendanceState.TARDANZA, 10, now);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenFieldsDiffer() {
        var now = LocalDateTime.now();
        var a = new AttendanceMarkedEvent("att-1", 1L, 2L, 3L, AttendanceState.PUNTUAL, 0, now);
        var b = new AttendanceMarkedEvent("att-2", 1L, 2L, 3L, AttendanceState.PUNTUAL, 0, now);
        assertNotEquals(a, b);
    }
}
