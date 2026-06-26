package trazzo.back.reports.domain.exception;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.UUID;

class MonthlyClosureNotFoundExceptionTest {

    @Test
    void shouldCreateExceptionWithMessage() {
        UUID id = UUID.randomUUID();
        MonthlyClosureNotFoundException exception = new MonthlyClosureNotFoundException(id);

        assertTrue(exception.getMessage().contains(id.toString()));
        assertInstanceOf(RuntimeException.class, exception);
    }
}
