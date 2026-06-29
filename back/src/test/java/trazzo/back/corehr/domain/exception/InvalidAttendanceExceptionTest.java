package trazzo.back.corehr.domain.exception;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class InvalidAttendanceExceptionTest {

    @Test
    void shouldCreateInvalidAttendanceExceptionWithMessage() {
        var ex = new InvalidAttendanceException("Attendance already has a check-out");
        assertEquals("Attendance already has a check-out", ex.getMessage());
        assertInstanceOf(IllegalStateException.class, ex);
    }
}
