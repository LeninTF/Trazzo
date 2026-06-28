package trazzo.back.incidents.domain.specification;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import trazzo.back.incidents.domain.model.IncidentEvidence;

import java.time.*;

class EvidenceDeletionWindowSpecTest {

    @Test
    void defaultWindowIs15Minutes() {
        var spec = new EvidenceDeletionWindowSpec();
        var now = LocalDateTime.now();
        var evidence = IncidentEvidence.restore("ev-1", "inc-1", "doc.pdf", "http://url", "pdf",
                100, false, null, now, now, now);
        var clock = Clock.fixed(now.plusMinutes(14).atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        assertTrue(spec.isSatisfiedBy(evidence, clock));
    }

    @Test
    void evidenceOutsideWindowIsNotSatisfied() {
        var spec = new EvidenceDeletionWindowSpec();
        var uploadedAt = LocalDateTime.now();
        var evidence = IncidentEvidence.restore("ev-1", "inc-1", "doc.pdf", "http://url", "pdf",
                100, false, null, uploadedAt, uploadedAt, uploadedAt);
        var clock = Clock.fixed(uploadedAt.plusMinutes(16).atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        assertFalse(spec.isSatisfiedBy(evidence, clock));
    }

    @Test
    void evidenceExactlyAtDeadlineIsSatisfied() {
        var spec = new EvidenceDeletionWindowSpec();
        var uploadedAt = LocalDateTime.now();
        var evidence = IncidentEvidence.restore("ev-1", "inc-1", "doc.pdf", "http://url", "pdf",
                100, false, null, uploadedAt, uploadedAt, uploadedAt);
        var clock = Clock.fixed(uploadedAt.plusMinutes(15).atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        assertTrue(spec.isSatisfiedBy(evidence, clock));
    }

    @Test
    void nullEvidenceIsNotSatisfied() {
        var spec = new EvidenceDeletionWindowSpec();
        assertFalse(spec.isSatisfiedBy(null, Clock.systemDefaultZone()));
    }

    @Test
    void evidenceWithBackfilledUploadedAtIsSatisfied() {
        var spec = new EvidenceDeletionWindowSpec();
        var now = LocalDateTime.now();
        var evidence = IncidentEvidence.restore("ev-1", "inc-1", "doc.pdf", "http://url", "pdf",
                100, false, null, null, now, now);
        assertNotNull(evidence.getUploadedAt());
        assertTrue(spec.isSatisfiedBy(evidence, Clock.fixed(now.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault())));
    }

    @Test
    void customWindowDuration() {
        var spec = new EvidenceDeletionWindowSpec(Duration.ofHours(1));
        var uploadedAt = LocalDateTime.now();
        var evidence = IncidentEvidence.restore("ev-1", "inc-1", "doc.pdf", "http://url", "pdf",
                100, false, null, uploadedAt, uploadedAt, uploadedAt);
        var clock = Clock.fixed(uploadedAt.plusMinutes(59).atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        assertTrue(spec.isSatisfiedBy(evidence, clock));
    }

    @Test
    void nullWindowThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new EvidenceDeletionWindowSpec(null));
    }

    @Test
    void negativeWindowThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new EvidenceDeletionWindowSpec(Duration.ofMinutes(-1)));
    }

    @Test
    void zeroWindowThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new EvidenceDeletionWindowSpec(Duration.ZERO));
    }
}
