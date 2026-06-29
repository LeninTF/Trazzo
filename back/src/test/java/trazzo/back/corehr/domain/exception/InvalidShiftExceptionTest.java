package trazzo.back.corehr.domain.exception;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class InvalidShiftExceptionTest {

    @Test
    void shouldCreateInvalidShiftExceptionWithMessage() {
        var ex = new InvalidShiftException("Shift name is required");
        assertEquals("Shift name is required", ex.getMessage());
        assertInstanceOf(IllegalArgumentException.class, ex);
    }
}
