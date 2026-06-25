package trazzo.back.incidents.infrastructure.adapters.in.web.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.util.List;

class ErrorResponseTest {

    @Test
    void createWithStatusAndErrorAndMessage() {
        var error = new ErrorResponse(400, "Bad Request", "Invalid input");
        assertEquals(400, error.status());
        assertEquals("Bad Request", error.error());
        assertEquals("Invalid input", error.message());
        assertNull(error.details());
        assertNotNull(error.timestamp());
    }

    @Test
    void createWithDetails() {
        var details = List.of(new ErrorResponse.ValidationDetail("nombre", "is required"));
        var error = new ErrorResponse(422, "Validation Error", "Failed", details);
        assertEquals(1, error.details().size());
        assertEquals("nombre", error.details().getFirst().field());
    }
}
