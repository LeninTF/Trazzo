package trazzo.back.reports.domain.exception;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.UUID;

class MonthlyClosureDetailNotFoundExceptionTest {

    @Test
    void shouldCreateExceptionWithMessage() {
        UUID id = UUID.randomUUID();
        MonthlyClosureDetailNotFoundException exception = new MonthlyClosureDetailNotFoundException(id);

        assertTrue(exception.getMessage().contains(id.toString()));
        assertInstanceOf(RuntimeException.class, exception);
    }
}
