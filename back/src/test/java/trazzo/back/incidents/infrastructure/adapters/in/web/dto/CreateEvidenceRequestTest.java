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
        var request = new CreateEvidenceRequest("doc.pdf", "file-key", "application/pdf", 100);
        var violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void blankFileNameFailsValidation() {
        var request = new CreateEvidenceRequest(" ", "file-key", "application/pdf", 100);
        var violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void blankFileKeyFailsValidation() {
        var request = new CreateEvidenceRequest("doc.pdf", " ", "application/pdf", 100);
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
        var request = new CreateEvidenceRequest("doc.pdf", "file-key", "application/pdf", 0);
        var violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void negativeFileSizeFailsValidation() {
        var request = new CreateEvidenceRequest("doc.pdf", "file-key", "application/pdf", -1);
        var violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void disallowedMimeTypeFailsValidation() {
        var request = new CreateEvidenceRequest("doc.exe", "file-key", "application/x-msdownload", 100);
        var violations = validator.validate(request);
        assertEquals(1, violations.size());
        assertEquals("mimeType", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    void eachWhitelistedMimeTypePassesValidation() {
        for (String mime : java.util.List.of(
                "application/pdf",
                "image/png",
                "image/jpeg",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "video/mp4",
                "video/quicktime"
        )) {
            var request = new CreateEvidenceRequest("doc", "file-key", mime, 100);
            var violations = validator.validate(request);
            assertTrue(violations.isEmpty(), "Expected no violations for mime: " + mime);
        }
    }
}
