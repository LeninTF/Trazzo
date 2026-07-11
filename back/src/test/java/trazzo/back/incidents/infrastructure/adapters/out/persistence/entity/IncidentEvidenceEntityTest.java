package trazzo.back.incidents.infrastructure.adapters.out.persistence.entity;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

class IncidentEvidenceEntityTest {

    @Test
    void createInstance() {
        var now = LocalDateTime.now();
        var entity = new IncidentEvidenceEntity("ev-1", "inc-1", "doc.pdf",
                "http://url", "pdf", 100, false, null, now, now, now);

        assertEquals("ev-1", entity.getId());
        assertEquals("inc-1", entity.getIncidentId());
        assertEquals("doc.pdf", entity.getFileName());
        assertEquals("http://url", entity.getFileKey());
        assertEquals("pdf", entity.getMimeType());
        assertEquals(100, entity.getFileSize());
        assertFalse(entity.isDeleted());
    }

    @Test
    void settersWorkCorrectly() {
        var entity = new IncidentEvidenceEntity();
        entity.setId("ev-1");
        entity.setDeleted(true);
        entity.setFileName("test.pdf");

        assertEquals("ev-1", entity.getId());
        assertTrue(entity.isDeleted());
        assertEquals("test.pdf", entity.getFileName());
    }
}
