package trazzo.back.incidents.infrastructure.adapters.in.web.dto;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CreateEvidenceRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void validRequestPassesValidation() {
        var request = new CreateEvidenceRequest("doc.pdf", "file-key", "pdf", 100);
        var violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void blankFileNameFailsValidation() {
        var request = new CreateEvidenceRequest(" ", "file-key", "pdf", 100);
        var violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void blankFileKeyFailsValidation() {
        var request = new CreateEvidenceRequest("doc.pdf", " ", "pdf", 100);
        var violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void blankMimeTypeFailsValidation() {
        var request = new CreateEvidenceRequest("doc.pdf", "file-key", " ", 100);
        var violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void zeroFileSizeFailsValidation() {
        var request = new CreateEvidenceRequest("doc.pdf", "file-key", "pdf", 0);
        var violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void negativeFileSizeFailsValidation() {
        var request = new CreateEvidenceRequest("doc.pdf", "file-key", "pdf", -1);
        var violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }
}
