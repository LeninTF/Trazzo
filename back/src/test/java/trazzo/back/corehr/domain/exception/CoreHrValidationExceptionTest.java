package trazzo.back.corehr.domain.exception;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class CoreHrValidationExceptionTest {

    @Test
    void shouldCreateCoreHrValidationExceptionWithMessage() {
        var ex = new CoreHrValidationException("Validation failed");
        assertEquals("Validation failed", ex.getMessage());
        assertInstanceOf(IllegalArgumentException.class, ex);
    }
}
