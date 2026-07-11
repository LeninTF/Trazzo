package trazzo.back.shared.domain.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class StorageExceptionTest {

    @Test
    void shouldCreateExceptionWithMessage() {
        var ex = new StorageException("upload failed");

        assertEquals("upload failed", ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    void shouldCreateExceptionWithMessageAndCause() {
        var cause = new RuntimeException("io error");
        var ex = new StorageException("upload failed", cause);

        assertEquals("upload failed", ex.getMessage());
        assertSame(cause, ex.getCause());
    }

    @Test
    void shouldBeRuntimeException() {
        var ex = new StorageException("test");

        assertInstanceOf(RuntimeException.class, ex);
    }
}
