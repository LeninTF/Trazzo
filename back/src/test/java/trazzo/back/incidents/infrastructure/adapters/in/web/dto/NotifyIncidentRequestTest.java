package trazzo.back.incidents.infrastructure.adapters.in.web.dto;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class NotifyIncidentRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void validRequestPassesValidation() {
        var request = new NotifyIncidentRequest("JUSTIFICACION");
        var violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void blankTipoFailsValidation() {
        var request = new NotifyIncidentRequest(" ");
        var violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }
}
