package trazzo.back.audit.domain.exception;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class AuditValidationExceptionTest {

    @Test
    void shouldCreateWithMessage() {
        var ex = new AuditValidationException("Invalid audit data");
        assertEquals("Invalid audit data", ex.getMessage());
    }

}
