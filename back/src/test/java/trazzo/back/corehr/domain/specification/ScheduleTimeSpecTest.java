package trazzo.back.corehr.domain.specification;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.time.LocalTime;

class ScheduleTimeSpecTest {

    private final ScheduleTimeSpec spec = new ScheduleTimeSpec();

    @Test
    void shouldBeValidWhenEntryBeforeDeparture() {
        assertTrue(spec.isValidScheduleTime(LocalTime.of(8, 0), LocalTime.of(17, 0)));
    }

    @Test
    void shouldBeInvalidWhenEntryAfterDeparture() {
        assertFalse(spec.isValidScheduleTime(LocalTime.of(17, 0), LocalTime.of(8, 0)));
    }

    @Test
    void shouldBeInvalidWhenEntryEqualsDeparture() {
        assertFalse(spec.isValidScheduleTime(LocalTime.of(8, 0), LocalTime.of(8, 0)));
    }

    @Test
    void shouldBeInvalidWhenEntryIsNull() {
        assertFalse(spec.isValidScheduleTime(null, LocalTime.of(17, 0)));
    }

    @Test
    void shouldBeInvalidWhenDepartureIsNull() {
        assertFalse(spec.isValidScheduleTime(LocalTime.of(8, 0), null));
    }
}
