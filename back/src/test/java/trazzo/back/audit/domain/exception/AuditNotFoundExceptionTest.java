package trazzo.back.audit.domain.exception;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class AuditNotFoundExceptionTest {

    @Test
    void shouldCreateWithMessage() {
        var ex = new AuditNotFoundException("Entity not found");
        assertEquals("Entity not found", ex.getMessage());
    }

}
