package trazzo.back.incidents.infrastructure.adapters.out.persistence.entity;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

class IncidentEvidenceEntityTest {

    @Test
    void createInstance() {
        var now = LocalDateTime.now();
        var entity = new IncidentEvidenceEntity(1, 1, "doc.pdf",
                "evidences/2/1/uuid/doc.pdf", "application/pdf", 100, false, null, now, now, now);

        assertEquals(1, entity.getId());
        assertEquals(1, entity.getIncidentId());
        assertEquals("doc.pdf", entity.getFileName());
        assertEquals("evidences/2/1/uuid/doc.pdf", entity.getFileKey());
        assertEquals("application/pdf", entity.getMimeType());
        assertEquals(100, entity.getFileSize());
        assertFalse(entity.isDeleted());
        assertNull(entity.getDeletedAt());
    }

    @Test
    void settersWorkCorrectly() {
        var entity = new IncidentEvidenceEntity();
        entity.setId(1);
        entity.setDeleted(true);
        entity.setFileName("test.pdf");
        entity.setFileKey("evidences/2/1/uuid/test.pdf");

        assertEquals(1, entity.getId());
        assertTrue(entity.isDeleted());
        assertEquals("test.pdf", entity.getFileName());
        assertEquals("evidences/2/1/uuid/test.pdf", entity.getFileKey());
    }

    @Test
    void prePersistPopulatesTimestampsWhenMissing() {
        var entity = new IncidentEvidenceEntity();
        entity.setIncidentId(1);
        entity.setFileName("doc.pdf");
        entity.setFileKey("evidences/2/1/uuid/doc.pdf");
        entity.setMimeType("application/pdf");
        entity.setFileSize(100);
        entity.setDeleted(false);

        entity.onCreate();

        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
        assertNotNull(entity.getUploadedAt());
        assertEquals(entity.getCreatedAt(), entity.getUploadedAt());
    }

    @Test
    void prePersistKeepsProvidedUploadedAt() {
        var uploaded = LocalDateTime.of(2025, 1, 1, 10, 0);
        var entity = new IncidentEvidenceEntity();
        entity.setIncidentId(1);
        entity.setFileName("doc.pdf");
        entity.setFileKey("evidences/2/1/uuid/doc.pdf");
        entity.setMimeType("application/pdf");
        entity.setFileSize(100);
        entity.setDeleted(false);
        entity.setUploadedAt(uploaded);

        entity.onCreate();

        assertEquals(uploaded, entity.getUploadedAt());
        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
    }

    @Test
    void preUpdateRefreshesUpdatedAt() {
        var entity = new IncidentEvidenceEntity();
        entity.setIncidentId(1);
        entity.setFileName("doc.pdf");
        entity.setFileKey("evidences/2/1/uuid/doc.pdf");
        entity.setMimeType("application/pdf");
        entity.setFileSize(100);
        entity.setDeleted(false);
        var firstUpdate = LocalDateTime.of(2025, 1, 1, 10, 0);
        entity.setUpdatedAt(firstUpdate);

        entity.onUpdate();

        assertNotEquals(firstUpdate, entity.getUpdatedAt());
    }
}
