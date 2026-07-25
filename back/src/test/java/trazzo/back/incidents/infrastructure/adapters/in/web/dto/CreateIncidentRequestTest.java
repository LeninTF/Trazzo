package trazzo.back.incidents.infrastructure.adapters.in.web.dto;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CreateIncidentRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void validRequestPassesValidation() {
        var request = new CreateIncidentRequest("t-1", "comment");
        var violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void blankIncidenciaTypeIdFailsValidation() {
        var request = new CreateIncidentRequest(" ", "comment");
        var violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void nullCommentIsValid() {
        var request = new CreateIncidentRequest("t-1", null);
        var violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }
}
