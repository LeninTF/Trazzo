package trazzo.back.corehr.domain.exception;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class InvalidToleranciaExceptionTest {

    @Test
    void shouldCreateInvalidToleranciaExceptionWithMessage() {
        var ex = new InvalidToleranciaException("type is required");
        assertEquals("type is required", ex.getMessage());
        assertInstanceOf(IllegalArgumentException.class, ex);
    }
}
