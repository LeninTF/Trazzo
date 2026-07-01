package trazzo.back.corehr.domain.exception;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class InactiveDeviceExceptionTest {

    @Test
    void shouldCreateInactiveDeviceExceptionWithMessage() {
        var ex = new InactiveDeviceException("Device is not active");
        assertEquals("Device is not active", ex.getMessage());
        assertInstanceOf(IllegalStateException.class, ex);
    }
}
