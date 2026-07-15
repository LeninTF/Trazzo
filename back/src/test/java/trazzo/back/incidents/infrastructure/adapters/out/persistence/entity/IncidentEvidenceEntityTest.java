package trazzo.back.incidents.infrastructure.adapters.out.persistence.entity;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

class IncidentEvidenceEntityTest {

    @Test
    void createInstance() {
        var now = LocalDateTime.now();
        var entity = new IncidentEvidenceEntity(1, 1, "doc.pdf",
                "http://url", "key-123", "pdf", 100, false, null, now, now, now);

        assertEquals(1, entity.getId());
        assertEquals(1, entity.getIncidentId());
        assertEquals("doc.pdf", entity.getFileName());
        assertEquals("http://url", entity.getFileUrl());
        assertEquals("key-123", entity.getFileKey());
        assertEquals("pdf", entity.getMimeType());
        assertEquals(100, entity.getFileSize());
        assertFalse(entity.isDeleted());
    }

    @Test
    void settersWorkCorrectly() {
        var entity = new IncidentEvidenceEntity();
        entity.setId(1);
        entity.setDeleted(true);
        entity.setFileName("test.pdf");

        assertEquals(1, entity.getId());
        assertTrue(entity.isDeleted());
        assertEquals("test.pdf", entity.getFileName());
    }
}
