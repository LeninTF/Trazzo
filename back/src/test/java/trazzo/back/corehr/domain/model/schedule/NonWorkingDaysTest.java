package trazzo.back.corehr.domain.model.schedule;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import trazzo.back.corehr.domain.exception.InvalidNonWorkingDaysException;
import java.time.LocalDate;

class NonWorkingDaysTest {

    @Test
    void shouldCreateNonWorkingDay() {
        var nwd = NonWorkingDays.create(LocalDate.of(2025, 1, 1), "New Year", true);
        assertNull(nwd.getId());
        assertEquals(LocalDate.of(2025, 1, 1), nwd.getDate());
        assertTrue(nwd.isRecurring());
    }

    @Test
    void shouldRestoreNonWorkingDay() {
        var now = java.time.LocalDateTime.now();
        var nwd = NonWorkingDays.restore(1L, LocalDate.of(2025, 1, 1), "New Year", true, now);
        assertEquals(1L, nwd.getId());
        assertEquals("New Year", nwd.getDescription());
    }

    @Test
    void shouldUpdateDescription() {
        var nwd = NonWorkingDays.create(LocalDate.of(2025, 1, 1), "Old", false);
        nwd.updateDescription("Updated");
        assertEquals("Updated", nwd.getDescription());
    }

    @Test
    void shouldThrowWhenDateIsNull() {
        assertThrows(InvalidNonWorkingDaysException.class, () ->
            NonWorkingDays.create(null, "desc", false)
        );
    }
}
