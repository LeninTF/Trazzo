package trazzo.back.incidents.infrastructure.adapters.in.web.dto;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CreateIncidentTypeRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void validRequestPassesValidation() {
        var request = new CreateIncidentTypeRequest("Permiso", "Descripcion");
        var violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void blankNombreFailsValidation() {
        var request = new CreateIncidentTypeRequest(" ", "Desc");
        var violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void validRequestWithNullDescripcion() {
        var request = new CreateIncidentTypeRequest("Permiso", null);
        var violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }
}
