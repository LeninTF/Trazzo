package trazzo.back.incidents.domain.specification;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import trazzo.back.incidents.domain.model.IncidentEvidence;

import java.time.LocalDateTime;
import java.util.List;

class IncidentEvidenceSpecTest {

    private final IncidentEvidenceSpec spec = new IncidentEvidenceSpec();

    @Test
    void allowsSingleActiveEvidenceWithNullCollection() {
        assertTrue(spec.allowsSingleActiveEvidence(null));
    }

    @Test
    void allowsSingleActiveEvidenceWithEmptyCollection() {
        assertTrue(spec.allowsSingleActiveEvidence(List.of()));
    }

    @Test
    void allowsSingleActiveEvidenceWithOneActive() {
        var ev = IncidentEvidence.restore("ev-1", "inc-1", "doc.pdf", "http://url", "pdf", 100, false, null, LocalDateTime.now(), LocalDateTime.now());
        assertTrue(spec.allowsSingleActiveEvidence(List.of(ev)));
    }

    @Test
    void allowsSingleActiveEvidenceWithAllDeleted() {
        var now = LocalDateTime.now();
        var ev1 = IncidentEvidence.restore("ev-1", "inc-1", "a.pdf", "http://a", "pdf", 100, true, now, now, now);
        var ev2 = IncidentEvidence.restore("ev-2", "inc-1", "b.pdf", "http://b", "pdf", 200, true, now, now, now);
        assertTrue(spec.allowsSingleActiveEvidence(List.of(ev1, ev2)));
    }

    @Test
    void rejectsMultipleActiveEvidences() {
        var now = LocalDateTime.now();
        var ev1 = IncidentEvidence.restore("ev-1", "inc-1", "a.pdf", "http://a", "pdf", 100, false, null, now, now, now);
        var ev2 = IncidentEvidence.restore("ev-2", "inc-1", "b.pdf", "http://b", "pdf", 200, false, null, now, now, now);
        assertFalse(spec.allowsSingleActiveEvidence(List.of(ev1, ev2)));
    }

    @Test
    void validFileSize() {
        assertTrue(spec.isValidFileSize(1));
        assertTrue(spec.isValidFileSize(IncidentEvidenceSpec.MAX_FILE_SIZE_BYTES));
    }

    @Test
    void invalidFileSizeWithZero() {
        assertFalse(spec.isValidFileSize(0));
    }

    @Test
    void invalidFileSizeWithNegative() {
        assertFalse(spec.isValidFileSize(-1));
    }

    @Test
    void invalidFileSizeExceedingMax() {
        assertFalse(spec.isValidFileSize(IncidentEvidenceSpec.MAX_FILE_SIZE_BYTES + 1));
    }

    @Test
    void isAllowedMimeTypeAcceptsWhitelistedTypes() {
        for (String mime : IncidentEvidenceSpec.ALLOWED_MIME_TYPES) {
            assertTrue(spec.isAllowedMimeType(mime), "Expected to accept: " + mime);
        }
    }

    @Test
    void isAllowedMimeTypeIsCaseInsensitive() {
        assertTrue(spec.isAllowedMimeType("APPLICATION/PDF"));
        assertTrue(spec.isAllowedMimeType("Image/PNG"));
    }

    @Test
    void isAllowedMimeTypeRejectsUnknown() {
        assertFalse(spec.isAllowedMimeType("application/x-msdownload"));
    }

    @Test
    void isAllowedMimeTypeRejectsNull() {
        assertFalse(spec.isAllowedMimeType(null));
    }
}
