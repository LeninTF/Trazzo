package trazzo.back.corehr.domain.model.schedule;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import trazzo.back.corehr.domain.exception.InvalidShiftException;

class ShiftTest {

    @Test
    void shouldCreateShift() {
        var shift = Shift.create("Morning", "Morning shift");
        assertNull(shift.getId());
        assertEquals("Morning", shift.getName());
        assertEquals("Morning shift", shift.getDescription());
        assertNotNull(shift.getCreatedAt());
        assertNotNull(shift.getUpdatedAt());
    }

    @Test
    void shouldRestoreShift() {
        var now = java.time.LocalDateTime.now();
        var shift = Shift.restore(1L, "Morning", "Desc", now, now);
        assertEquals(1L, shift.getId());
        assertEquals("Morning", shift.getName());
        assertEquals("Desc", shift.getDescription());
    }

    @Test
    void shouldRenameShift() {
        var shift = Shift.create("Morning", null);
        shift.rename("Afternoon");
        assertEquals("Afternoon", shift.getName());
    }

    @Test
    void shouldUpdateDescription() {
        var shift = Shift.create("Morning", null);
        shift.updateDescription("New description");
        assertEquals("New description", shift.getDescription());
    }

    @Test
    void shouldSetNullDescriptionWhenBlank() {
        var shift = Shift.create("Morning", "  ");
        assertNull(shift.getDescription());
    }

    @Test
    void shouldThrowWhenNameIsNull() {
        assertThrows(InvalidShiftException.class, () -> Shift.create(null, null));
    }

    @Test
    void shouldThrowWhenNameIsBlank() {
        assertThrows(InvalidShiftException.class, () -> Shift.create("  ", null));
    }
}
