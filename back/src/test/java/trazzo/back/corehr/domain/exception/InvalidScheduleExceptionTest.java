package trazzo.back.corehr.domain.exception;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class InvalidScheduleExceptionTest {

    @Test
    void shouldCreateInvalidScheduleExceptionWithMessage() {
        var ex = new InvalidScheduleException("departureTime must be after entryTime");
        assertEquals("departureTime must be after entryTime", ex.getMessage());
        assertInstanceOf(IllegalArgumentException.class, ex);
    }
}
