package trazzo.back.incidents.infrastructure.adapters.in.web.dto;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import trazzo.back.incidents.domain.model.IncidentState;

class IncidentStateChangeRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void validRequestPassesValidation() {
        var request = new IncidentStateChangeRequest(IncidentState.APROBADO, 3, null);
        var violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void nullStateFailsValidation() {
        var request = new IncidentStateChangeRequest(null, null, null);
        var violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void rejectWithMotivo() {
        var request = new IncidentStateChangeRequest(IncidentState.DENEGADO, null, "motivo");
        var violations = validator.validate(request);
        assertTrue(violations.isEmpty());
        assertEquals("motivo", request.motivoRechazo());
    }

    @Test
    void rejectWithoutMotivoFailsValidation() {
        var request = new IncidentStateChangeRequest(IncidentState.DENEGADO, null, null);
        var violations = validator.validate(request);
        assertEquals(1, violations.size());
        var violation = violations.iterator().next();
        assertEquals("motivo_rechazo", violation.getPropertyPath().toString());
    }

    @Test
    void rejectWithBlankMotivoFailsValidation() {
        var request = new IncidentStateChangeRequest(IncidentState.DENEGADO, null, "   ");
        var violations = validator.validate(request);
        assertEquals(1, violations.size());
        assertEquals("motivo_rechazo", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    void approveWithDaysGranted() {
        var request = new IncidentStateChangeRequest(IncidentState.APROBADO, 5, null);
        assertEquals(5, request.daysGranted());
    }
}
