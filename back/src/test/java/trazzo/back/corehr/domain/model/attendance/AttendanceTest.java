package trazzo.back.corehr.domain.model.attendance;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import trazzo.back.corehr.domain.exception.InvalidAttendanceException;
import trazzo.back.corehr.domain.model.AttendanceState;
import java.time.LocalTime;

class AttendanceTest {

    @Test
    void shouldRegisterCheckIn() {
        var a = Attendance.registerCheckIn(1L, 1L, 1L, LocalTime.of(8, 0), 10);
        assertNotNull(a.getId());
        assertEquals(1L, a.getTenantUserId());
        assertNotNull(a.getCheckIn());
        assertNull(a.getCheckOut());
        assertNotNull(a.getAttendanceDate());
        assertNotNull(a.getState());
    }

    @Test
    void shouldRegisterCheckInLateWithoutTolerance() {
        var scheduledEntry = LocalTime.of(8, 0);
        var a = Attendance.registerCheckIn(1L, 1L, 1L, scheduledEntry, 0);
        assertTrue(a.getMinutesLate() >= 0);
        if (a.getCheckIn().toLocalTime().isAfter(scheduledEntry)) {
            assertEquals(AttendanceState.TARDANZA, a.getState());
        }
    }

    @Test
    void shouldRegisterCheckOut() {
        var a = Attendance.registerCheckIn(1L, 1L, 1L, LocalTime.of(8, 0), 10);
        a.registerCheckOut();
        assertNotNull(a.getCheckOut());
        assertTrue(a.isComplete());
    }

    @Test
    void shouldThrowWhenDuplicateCheckOut() {
        var a = Attendance.registerCheckIn(1L, 1L, 1L, LocalTime.of(8, 0), 10);
        a.registerCheckOut();
        assertThrows(InvalidAttendanceException.class, a::registerCheckOut);
    }

    @Test
    void shouldCorrectAttendance() {
        var a = Attendance.registerCheckIn(1L, 1L, 1L, LocalTime.of(8, 0), 10);
        a.correct(15, AttendanceState.TARDANZA);
        assertEquals(15, a.getMinutesLate());
        assertEquals(AttendanceState.TARDANZA, a.getState());
    }

    @Test
    void shouldPullDomainEventsAfterCheckIn() {
        var a = Attendance.registerCheckIn(1L, 1L, 1L, LocalTime.of(8, 0), 10);
        var events = a.pullDomainEvents();
        assertEquals(1, events.size());
        assertTrue(events.get(0) instanceof trazzo.back.corehr.domain.event.AttendanceRegisteredEvent);
        assertTrue(a.pullDomainEvents().isEmpty());
    }

    @Test
    void shouldPullDomainEventsAfterCheckOut() {
        var a = Attendance.registerCheckIn(1L, 1L, 1L, LocalTime.of(8, 0), 10);
        a.pullDomainEvents();
        a.registerCheckOut();
        var events = a.pullDomainEvents();
        assertEquals(1, events.size());
        assertTrue(events.get(0) instanceof trazzo.back.corehr.domain.event.AttendanceCompletedEvent);
    }

    @Test
    void shouldPullDomainEventsAfterCorrection() {
        var a = Attendance.registerCheckIn(1L, 1L, 1L, LocalTime.of(8, 0), 10);
        a.pullDomainEvents();
        a.correct(5, AttendanceState.TARDANZA);
        var events = a.pullDomainEvents();
        assertEquals(1, events.size());
        assertTrue(events.get(0) instanceof trazzo.back.corehr.domain.event.AttendanceCorrectedEvent);
    }

    @Test
    void shouldRestoreAttendance() {
        var now = java.time.LocalDateTime.now();
        var a = Attendance.restore(
                "test-id", 1L, 1L, 1L,
                now.minusHours(8), now, now.toLocalDate(),
                0, AttendanceState.PUNTUAL, null, null, now, now
        );
        assertEquals("test-id", a.getId());
        assertTrue(a.isComplete());
    }

    @Test
    void shouldThrowWhenMinutesLateNegativeOnCorrection() {
        var a = Attendance.registerCheckIn(1L, 1L, 1L, LocalTime.of(8, 0), 10);
        assertThrows(InvalidAttendanceException.class, () -> a.correct(-1, AttendanceState.PUNTUAL));
    }

    @Test
    void shouldThrowWhenStateNullOnCorrection() {
        var a = Attendance.registerCheckIn(1L, 1L, 1L, LocalTime.of(8, 0), 10);
        assertThrows(InvalidAttendanceException.class, () -> a.correct(0, null));
    }

    @Test
    void shouldRegisterCheckInWithoutScheduleEntryTime() {
        var a = Attendance.registerCheckIn(1L, null, 1L, null, 0);
        assertEquals(AttendanceState.PUNTUAL, a.getState());
        assertEquals(0, a.getMinutesLate());
    }
}
