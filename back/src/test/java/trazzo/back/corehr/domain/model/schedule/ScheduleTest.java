package trazzo.back.corehr.domain.model.schedule;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import trazzo.back.corehr.domain.exception.InvalidScheduleException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

class ScheduleTest {

    @Test
    void shouldCreateSchedule() {
        var s = Schedule.create(1L, "Morning", "Desc", LocalTime.of(8, 0), LocalTime.of(17, 0), List.of(DayOfWeek.MONDAY));
        assertNull(s.getId());
        assertEquals(1L, s.getShiftId());
        assertEquals("Morning", s.getName());
        assertEquals(LocalTime.of(8, 0), s.getEntryTime());
        assertEquals(LocalTime.of(17, 0), s.getDepartureTime());
        assertEquals(List.of(DayOfWeek.MONDAY), s.getDaysOfWeek());
    }

    @Test
    void shouldRestoreSchedule() {
        var now = java.time.LocalDateTime.now();
        var s = Schedule.restore(1L, 1L, "Morning", "Desc", LocalTime.of(8, 0), LocalTime.of(17, 0),
                List.of(DayOfWeek.TUESDAY), now, now);
        assertEquals(1L, s.getId());
        assertEquals("Morning", s.getName());
        assertEquals(List.of(DayOfWeek.TUESDAY), s.getDaysOfWeek());
    }

    @Test
    void shouldReschedule() {
        var s = Schedule.create(1L, "Morning", null, LocalTime.of(8, 0), LocalTime.of(17, 0), null);
        s.reschedule(LocalTime.of(9, 0), LocalTime.of(18, 0));
        assertEquals(LocalTime.of(9, 0), s.getEntryTime());
        assertEquals(LocalTime.of(18, 0), s.getDepartureTime());
    }

    @Test
    void shouldRename() {
        var s = Schedule.create(1L, "Morning", null, LocalTime.of(8, 0), LocalTime.of(17, 0), null);
        s.rename("Afternoon");
        assertEquals("Afternoon", s.getName());
    }

    @Test
    void shouldThrowWhenEntryEqualsDeparture() {
        assertThrows(InvalidScheduleException.class, () ->
            Schedule.create(1L, "Bad", null, LocalTime.of(17, 0), LocalTime.of(17, 0), null)
        );
    }

    @Test
    void shouldThrowWhenNameIsNull() {
        assertThrows(InvalidScheduleException.class, () ->
            Schedule.create(1L, null, null, LocalTime.of(8, 0), LocalTime.of(17, 0), null)
        );
    }

    @Test
    void shouldThrowWhenEntryTimeIsNull() {
        assertThrows(InvalidScheduleException.class, () ->
            Schedule.create(1L, "Name", null, null, LocalTime.of(17, 0), null)
        );
    }
}
