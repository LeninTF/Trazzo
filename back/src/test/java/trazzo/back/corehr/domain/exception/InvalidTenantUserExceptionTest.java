package trazzo.back.corehr.domain.exception;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class InvalidTenantUserExceptionTest {

    @Test
    void shouldCreateInvalidTenantUserExceptionWithMessage() {
        var ex = new InvalidTenantUserException("masterUserId is required");
        assertEquals("masterUserId is required", ex.getMessage());
        assertInstanceOf(IllegalArgumentException.class, ex);
    }
}
