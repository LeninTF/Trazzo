package trazzo.back.corehr.domain.model.attendance;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import trazzo.back.corehr.domain.event.AttendanceCompletedEvent;
import trazzo.back.corehr.domain.event.AttendanceRegisteredEvent;
import trazzo.back.corehr.domain.exception.InvalidAttendanceException;
import trazzo.back.corehr.domain.model.AttendanceState;
import java.time.LocalDateTime;
import java.time.LocalTime;

class AttendanceRegisterCheckInAtTest {

    @Test
    void registerCheckInAtShouldUseCapturedAtForCheckIn() {
        var capturedAt = LocalDateTime.of(2026, 7, 19, 8, 15);
        var a = Attendance.registerCheckInAt(1L, 1L, 1L, LocalTime.of(8, 0), 10, capturedAt);
        assertNotNull(a.getId());
        assertEquals(1L, a.getTenantUserId());
        assertEquals(1L, a.getScheduleId());
        assertEquals(1L, a.getDeviceId());
        assertEquals(capturedAt, a.getCheckIn());
        assertNull(a.getCheckOut());
        assertEquals(capturedAt.toLocalDate(), a.getAttendanceDate());
        assertEquals(capturedAt, a.getCreatedAt());
        assertEquals(capturedAt, a.getUpdatedAt());
    }

    @Test
    void registerCheckInAtShouldSetPuntualWhenWithinTolerance() {
        var capturedAt = LocalDateTime.of(2026, 7, 19, 8, 5);
        var a = Attendance.registerCheckInAt(1L, 1L, 1L, LocalTime.of(8, 0), 10, capturedAt);
        assertEquals(AttendanceState.PUNTUAL, a.getState());
        assertEquals(0, a.getMinutesLate());
    }

    @Test
    void registerCheckInAtShouldSetTardanzaWhenExceedingTolerance() {
        var capturedAt = LocalDateTime.of(2026, 7, 19, 8, 25);
        var a = Attendance.registerCheckInAt(1L, 1L, 1L, LocalTime.of(8, 0), 10, capturedAt);
        assertEquals(AttendanceState.TARDANZA, a.getState());
        assertEquals(15, a.getMinutesLate());
    }

    @Test
    void registerCheckInAtShouldSetPuntualWhenArrivingBeforeScheduledTime() {
        var capturedAt = LocalDateTime.of(2026, 7, 19, 7, 50);
        var a = Attendance.registerCheckInAt(1L, 1L, 1L, LocalTime.of(8, 0), 10, capturedAt);
        assertEquals(AttendanceState.PUNTUAL, a.getState());
        assertEquals(0, a.getMinutesLate());
    }

    @Test
    void registerCheckInAtShouldSetPuntualWhenScheduleEntryTimeIsNull() {
        var capturedAt = LocalDateTime.of(2026, 7, 19, 10, 30);
        var a = Attendance.registerCheckInAt(1L, null, 1L, null, 0, capturedAt);
        assertEquals(AttendanceState.PUNTUAL, a.getState());
        assertEquals(0, a.getMinutesLate());
    }

    @Test
    void registerCheckInAtShouldRecordAttendanceRegisteredEvent() {
        var capturedAt = LocalDateTime.of(2026, 7, 19, 8, 0);
        var a = Attendance.registerCheckInAt(1L, 1L, 1L, LocalTime.of(8, 0), 10, capturedAt);
        var events = a.pullDomainEvents();
        assertEquals(1, events.size());
        assertInstanceOf(AttendanceRegisteredEvent.class, events.get(0));
    }

    @Test
    void registerCheckOutAtShouldSetCheckOutTime() {
        var capturedIn = LocalDateTime.of(2026, 7, 19, 8, 0);
        var a = Attendance.registerCheckInAt(1L, 1L, 1L, LocalTime.of(8, 0), 10, capturedIn);
        a.pullDomainEvents();

        var capturedOut = LocalDateTime.of(2026, 7, 19, 17, 0);
        a.registerCheckOutAt(capturedOut);

        assertEquals(capturedOut, a.getCheckOut());
        assertTrue(a.isComplete());
    }

    @Test
    void registerCheckOutAtShouldRecordAttendanceCompletedEvent() {
        var capturedIn = LocalDateTime.of(2026, 7, 19, 8, 0);
        var a = Attendance.registerCheckInAt(1L, 1L, 1L, LocalTime.of(8, 0), 10, capturedIn);
        a.pullDomainEvents();

        a.registerCheckOutAt(LocalDateTime.of(2026, 7, 19, 17, 0));
        var events = a.pullDomainEvents();
        assertEquals(1, events.size());
        assertInstanceOf(AttendanceCompletedEvent.class, events.get(0));
    }

    @Test
    void registerCheckOutAtShouldThrowWhenCalledTwice() {
        var capturedIn = LocalDateTime.of(2026, 7, 19, 8, 0);
        var a = Attendance.registerCheckInAt(1L, 1L, 1L, LocalTime.of(8, 0), 10, capturedIn);
        a.registerCheckOutAt(LocalDateTime.of(2026, 7, 19, 17, 0));

        assertThrows(InvalidAttendanceException.class,
                () -> a.registerCheckOutAt(LocalDateTime.of(2026, 7, 19, 18, 0)));
    }

    @Test
    void registerCheckOutAtShouldThrowWhenCapturedAtIsBeforeAttendanceDate() {
        var capturedIn = LocalDateTime.of(2026, 7, 19, 8, 0);
        var a = Attendance.registerCheckInAt(1L, 1L, 1L, LocalTime.of(8, 0), 10, capturedIn);

        var capturedOutBefore = LocalDateTime.of(2026, 7, 18, 17, 0);
        assertThrows(InvalidAttendanceException.class,
                () -> a.registerCheckOutAt(capturedOutBefore));
    }

    @Test
    void registerCheckInAtShouldSetTardanzaWithZeroTolerance() {
        var capturedAt = LocalDateTime.of(2026, 7, 19, 8, 1);
        var a = Attendance.registerCheckInAt(1L, 1L, 1L, LocalTime.of(8, 0), 0, capturedAt);
        assertEquals(AttendanceState.TARDANZA, a.getState());
        assertEquals(1, a.getMinutesLate());
    }

    @Test
    void registerCheckInAtShouldHandleExactScheduledTime() {
        var capturedAt = LocalDateTime.of(2026, 7, 19, 8, 0);
        var a = Attendance.registerCheckInAt(1L, 1L, 1L, LocalTime.of(8, 0), 10, capturedAt);
        assertEquals(AttendanceState.PUNTUAL, a.getState());
        assertEquals(0, a.getMinutesLate());
    }

    @Test
    void registerCheckInAtAndCheckOutAtShouldMarkAttendanceComplete() {
        var capturedIn = LocalDateTime.of(2026, 7, 19, 8, 0);
        var a = Attendance.registerCheckInAt(1L, 1L, 1L, LocalTime.of(8, 0), 10, capturedIn);
        assertFalse(a.isComplete());

        a.registerCheckOutAt(LocalDateTime.of(2026, 7, 19, 17, 0));
        assertTrue(a.isComplete());
    }
}
