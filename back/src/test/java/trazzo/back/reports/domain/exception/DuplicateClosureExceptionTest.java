package trazzo.back.reports.domain.exception;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class DuplicateClosureExceptionTest {

    @Test
    void shouldCreateExceptionWithMessage() {
        DuplicateClosureException exception = new DuplicateClosureException(6, 2025);

        assertTrue(exception.getMessage().contains("6"));
        assertTrue(exception.getMessage().contains("2025"));
        assertInstanceOf(RuntimeException.class, exception);
    }
}
