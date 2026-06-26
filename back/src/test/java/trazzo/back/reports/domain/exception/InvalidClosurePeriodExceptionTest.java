package trazzo.back.reports.domain.exception;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class InvalidClosurePeriodExceptionTest {

    @Test
    void shouldCreateExceptionWithMessage() {
        InvalidClosurePeriodException exception = new InvalidClosurePeriodException("Month must be between 1 and 12");

        assertEquals("Month must be between 1 and 12", exception.getMessage());
        assertInstanceOf(RuntimeException.class, exception);
    }
}
