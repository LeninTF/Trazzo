package trazzo.back.corehr.domain.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class InvalidShiftExceptionTest {

    @Test
    void shouldCreateInvalidShiftExceptionWithMessage() {
        // Arrange (Preparar)
        String expectedMessage = "El turno indicado no es válido";

        // Act (Actuar) - Solo se instancia, NO se lanza
        InvalidShiftException exception = new InvalidShiftException(expectedMessage);

        // Assert (Verificar)
        assertNotNull(exception);
        assertEquals(expectedMessage, exception.getMessage());
    }
}