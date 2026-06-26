package trazzo.back.reports.domain.model.closure;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import trazzo.back.reports.domain.exception.InvalidClosurePeriodException;

class ClosurePeriodTest {

    @Test
    void shouldCreateValidPeriod() {
        ClosurePeriod period = new ClosurePeriod(6, 2025);
        assertEquals(6, period.month());
        assertEquals(2025, period.year());
    }

    @ParameterizedTest
    @CsvSource({ "0, 2025", "13, 2025", "1, 1999", "12, 1000" })
    void shouldThrowExceptionForInvalidPeriod(int month, int year) {
        assertThrows(InvalidClosurePeriodException.class, () -> new ClosurePeriod(month, year));
    }

    @Test
    void shouldBeEqualForSameValues() {
        ClosurePeriod p1 = new ClosurePeriod(6, 2025);
        ClosurePeriod p2 = new ClosurePeriod(6, 2025);
        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    void shouldNotBeEqualForDifferentValues() {
        ClosurePeriod p1 = new ClosurePeriod(6, 2025);
        ClosurePeriod p2 = new ClosurePeriod(7, 2025);
        assertNotEquals(p1, p2);
    }

    @Test
    void shouldReturnToString() {
        ClosurePeriod period = new ClosurePeriod(6, 2025);
        assertNotNull(period.toString());
        assertTrue(period.toString().contains("6"));
        assertTrue(period.toString().contains("2025"));
    }
}
