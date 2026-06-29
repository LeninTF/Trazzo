package trazzo.back.corehr.domain.specification;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.time.LocalTime;
import java.util.List;

class AttendanceToleranceSpecTest {

    private final AttendanceToleranceSpec spec = new AttendanceToleranceSpec();

    @Test
    void shouldReturnZeroWhenOnTime() {
        var late = spec.calculateMinutesLate(LocalTime.of(8, 0), LocalTime.of(8, 0), List.of(0));
        assertEquals(0, late);
    }

    @Test
    void shouldReturnZeroWithinTolerance() {
        var late = spec.calculateMinutesLate(LocalTime.of(8, 0), LocalTime.of(8, 5), List.of(10));
        assertEquals(0, late);
    }

    @Test
    void shouldCalculateMinutesLateBeyondTolerance() {
        var late = spec.calculateMinutesLate(LocalTime.of(8, 0), LocalTime.of(8, 15), List.of(5));
        assertEquals(10, late);
    }

    @Test
    void shouldReturnZeroWhenScheduledIsNull() {
        var late = spec.calculateMinutesLate(null, LocalTime.of(8, 0), List.of(0));
        assertEquals(0, late);
    }

    @Test
    void shouldUseMaxToleranceWhenMultiple() {
        var late = spec.calculateMinutesLate(LocalTime.of(8, 0), LocalTime.of(8, 20), List.of(5, 10, 15));
        assertEquals(5, late);
    }

    @Test
    void shouldHandleEmptyToleranceList() {
        var late = spec.calculateMinutesLate(LocalTime.of(8, 0), LocalTime.of(8, 10), List.of());
        assertEquals(10, late);
    }
}
