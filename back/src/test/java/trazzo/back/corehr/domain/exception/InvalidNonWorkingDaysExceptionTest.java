package trazzo.back.corehr.domain.exception;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class InvalidNonWorkingDaysExceptionTest {

    @Test
    void shouldCreateInvalidNonWorkingDaysExceptionWithMessage() {
        var ex = new InvalidNonWorkingDaysException("date is required");
        assertEquals("date is required", ex.getMessage());
        assertInstanceOf(IllegalArgumentException.class, ex);
    }
}
