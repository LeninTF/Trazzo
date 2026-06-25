package trazzo.back.incidents.infrastructure.adapters.in.web;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import trazzo.back.incidents.domain.exception.*;
import trazzo.back.incidents.infrastructure.adapters.in.web.dto.ErrorResponse;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleIllegalArgumentReturns400() {
        var ex = new IllegalArgumentException("invalid");
        var response = handler.handleIllegalArgument(ex);

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("invalid", response.getBody().message());
    }

    @Test
    void handleIllegalStateReturns400() {
        var ex = new IllegalStateException("bad state");
        var response = handler.handleIllegalState(ex);

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void handleIncidentValidationException() {
        var ex = new IncidentValidationException("validation failed");
        var response = handler.handleValidation(ex);

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void handleInvalidIncidentStateException() {
        var ex = new InvalidIncidentStateException("invalid state");
        var response = handler.handleInvalidState(ex);

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void handleInvalidIncidentEvidenceException() {
        var ex = new InvalidIncidentEvidenceException("invalid evidence");
        var response = handler.handleInvalidEvidence(ex);

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void handleInvalidIncidentPermissionException() {
        var ex = new InvalidIncidentPermissionException("invalid permission");
        var response = handler.handleInvalidPermission(ex);

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void handleInactiveIncidentTypeException() {
        var ex = new InactiveIncidentTypeException("inactive type");
        var response = handler.handleInactiveType(ex);

        assertEquals(400, response.getStatusCode().value());
    }
}
