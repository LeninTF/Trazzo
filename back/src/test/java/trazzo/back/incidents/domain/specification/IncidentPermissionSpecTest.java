package trazzo.back.incidents.domain.specification;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;

class IncidentPermissionSpecTest {

    private final IncidentPermissionSpec spec = new IncidentPermissionSpec();

    @Test
    void validPeriodWithStartBeforeEnd() {
        assertTrue(spec.hasValidPeriod(LocalDate.now(), LocalDate.now().plusDays(3)));
    }

    @Test
    void validPeriodWithSameStartAndEnd() {
        var date = LocalDate.now();
        assertTrue(spec.hasValidPeriod(date, date));
    }

    @Test
    void invalidPeriodWithEndBeforeStart() {
        assertFalse(spec.hasValidPeriod(LocalDate.now().plusDays(3), LocalDate.now()));
    }

    @Test
    void invalidPeriodWithNullStart() {
        assertFalse(spec.hasValidPeriod(null, LocalDate.now()));
    }

    @Test
    void invalidPeriodWithNullEnd() {
        assertFalse(spec.hasValidPeriod(LocalDate.now(), null));
    }

    @Test
    void validDaysGranted() {
        assertTrue(spec.hasValidDaysGranted(1));
        assertTrue(spec.hasValidDaysGranted(100));
    }

    @Test
    void invalidDaysGrantedWithZero() {
        assertFalse(spec.hasValidDaysGranted(0));
    }

    @Test
    void invalidDaysGrantedWithNegative() {
        assertFalse(spec.hasValidDaysGranted(-1));
    }
}
