package trazzo.back.incidents.infrastructure.adapters.in.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import trazzo.back.incidents.domain.exception.*;
import trazzo.back.incidents.infrastructure.adapters.in.web.dto.ErrorResponse;

import java.util.List;

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

    @Test
    void handleMethodArgumentNotValidReturns400() {
        var ex = mock(MethodArgumentNotValidException.class);
        var bindingResult = mock(BindingResult.class);
        var fieldError = new FieldError("obj", "nombre", "is required");
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        var response = handler.handleMethodArgumentNotValid(ex);

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().details().size());
        assertEquals("nombre", response.getBody().details().getFirst().field());
    }

    @Test
    void handleGenericReturns500() {
        var ex = new RuntimeException("Unexpected");
        var response = handler.handleGeneric(ex);

        assertEquals(500, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Internal Server Error", response.getBody().error());
        assertEquals("An unexpected error occurred", response.getBody().message());
    }

    @Test
    void handleValidationBodyHasCorrectMessage() {
        var ex = new IncidentValidationException("field is required");
        var response = handler.handleValidation(ex);

        assertEquals("field is required", response.getBody().message());
        assertEquals("Validation Error", response.getBody().error());
    }

    @Test
    void handleInvalidStateBodyHasCorrectMessage() {
        var ex = new InvalidIncidentStateException("cannot transition");
        var response = handler.handleInvalidState(ex);

        assertEquals("cannot transition", response.getBody().message());
        assertEquals("Invalid State", response.getBody().error());
    }

    @Test
    void handleInvalidEvidenceBodyHasCorrectMessage() {
        var ex = new InvalidIncidentEvidenceException("file too large");
        var response = handler.handleInvalidEvidence(ex);

        assertEquals("file too large", response.getBody().message());
        assertEquals("Invalid Evidence", response.getBody().error());
    }

    @Test
    void handleInvalidPermissionBodyHasCorrectMessage() {
        var ex = new InvalidIncidentPermissionException("not allowed");
        var response = handler.handleInvalidPermission(ex);

        assertEquals("not allowed", response.getBody().message());
        assertEquals("Invalid Permission", response.getBody().error());
    }

    @Test
    void handleInactiveTypeBodyHasCorrectMessage() {
        var ex = new InactiveIncidentTypeException("type is inactive");
        var response = handler.handleInactiveType(ex);

        assertEquals("type is inactive", response.getBody().message());
        assertEquals("Inactive Type", response.getBody().error());
    }
}
