package trazzo.back.incidents.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class IncidentEvidenceTest {

    /* == CREATION TESTS == */

    @Test
    void createWithValidFields() {
        var before = LocalDateTime.now();
        var evidence = IncidentEvidence.create(
                "inc-1", "documento.pdf", "http://files/doc.pdf",
                "application/pdf", 1024
        );
        var after = LocalDateTime.now();

        assertNotNull(evidence.getId());
        assertEquals("inc-1", evidence.getIncidentId());
        assertEquals("documento.pdf", evidence.getFileName());
        assertEquals("http://files/doc.pdf", evidence.getFileUrl());
        assertEquals("application/pdf", evidence.getMimeType());
        assertEquals(1024, evidence.getFileSize());
        assertFalse(evidence.isDeleted());
        assertNull(evidence.getDeletedAt());
        assertNotNull(evidence.getUploadedAt());
        assertNotNull(evidence.getCreatedAt());
        assertNotNull(evidence.getUpdatedAt());
        assertFalse(evidence.getUploadedAt().isBefore(before));
        assertFalse(evidence.getUploadedAt().isAfter(after));
        assertFalse(evidence.getCreatedAt().isBefore(before));
        assertFalse(evidence.getCreatedAt().isAfter(after));
        assertFalse(evidence.getUpdatedAt().isBefore(before));
        assertFalse(evidence.getUpdatedAt().isAfter(after));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void createWithBlankIncidentIdThrowsException(String incidentId) {
        assertThrows(
                IllegalArgumentException.class,
                () -> IncidentEvidence.create(
                        incidentId, "doc.pdf", "http://url", "pdf", 100
                )
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void createWithBlankFileNameThrowsException(String fileName) {
        assertThrows(
                IllegalArgumentException.class,
                () -> IncidentEvidence.create(
                        "inc-1", fileName, "http://url", "pdf", 100
                )
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void createWithBlankFileUrlThrowsException(String fileUrl) {
        assertThrows(
                IllegalArgumentException.class,
                () -> IncidentEvidence.create(
                        "inc-1", "doc.pdf", fileUrl, "pdf", 100
                )
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void createWithBlankMimeTypeThrowsException(String mimeType) {
        assertThrows(
                IllegalArgumentException.class,
                () -> IncidentEvidence.create(
                        "inc-1", "doc.pdf", "http://url", mimeType, 100
                )
        );
    }

    @Test
    void createWithZeroFileSizeThrowsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> IncidentEvidence.create("inc-1", "doc.pdf", "http://url", "pdf", 0)
        );
    }

    @Test
    void createWithNegativeFileSizeThrowsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> IncidentEvidence.create("inc-1", "doc.pdf", "http://url", "pdf", -1)
        );
    }

    @Test
    void createWithFileSizeExceedingMaxThrowsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> IncidentEvidence.create(
                        "inc-1", "doc.pdf", "http://url", "pdf",
                        IncidentEvidence.MAX_FILE_SIZE_BYTES + 1
                )
        );
    }

    @Test
    void createWithMaxFileSizeIsValid() {
        var evidence = IncidentEvidence.create(
                "inc-1", "doc.pdf", "http://url", "pdf",
                IncidentEvidence.MAX_FILE_SIZE_BYTES
        );
        assertEquals(IncidentEvidence.MAX_FILE_SIZE_BYTES, evidence.getFileSize());
    }

    /* == RESTORATION TESTS == */

    @Test
    void restoreWithAllFields() {
        var now = LocalDateTime.now();
        var evidence = IncidentEvidence.restore(
                "ev-1", "inc-1", "doc.pdf", "http://url", "pdf",
                1024, false, null, now, now
        );

        assertEquals("ev-1", evidence.getId());
        assertEquals("inc-1", evidence.getIncidentId());
        assertEquals("doc.pdf", evidence.getFileName());
        assertEquals("http://url", evidence.getFileUrl());
        assertEquals("pdf", evidence.getMimeType());
        assertEquals(1024, evidence.getFileSize());
        assertFalse(evidence.isDeleted());
        assertNull(evidence.getDeletedAt());
        assertEquals(now, evidence.getUploadedAt());
        assertEquals(now, evidence.getCreatedAt());
        assertEquals(now, evidence.getUpdatedAt());
    }

    @Test
    void restoreWithBlankIdNormalizesToNull() {
        var now = LocalDateTime.now();
        var evidence = IncidentEvidence.restore(
                " ", "inc-1", "doc.pdf", "http://url", "pdf",
                100, false, null, now, now
        );
        assertNull(evidence.getId());
    }

    @Test
    void restoreWithDeletedIsTrue() {
        var now = LocalDateTime.now();
        var evidence = IncidentEvidence.restore(
                "ev-1", "inc-1", "doc.pdf", "http://url", "pdf",
                100, true, now, now, now
        );

        assertTrue(evidence.isDeleted());
        assertEquals(now, evidence.getDeletedAt());
    }

    @Test
    void restoreWithNullDeletedAt() {
        var now = LocalDateTime.now();
        var evidence = IncidentEvidence.restore(
                "ev-1", "inc-1", "doc.pdf", "http://url", "pdf",
                100, true, null, now, now
        );

        assertTrue(evidence.isDeleted());
        assertNull(evidence.getDeletedAt());
    }

    @Test
    void restoreWithExplicitUploadedAtKeepsUploadTimestamp() {
        var uploadedAt = LocalDateTime.now().minusMinutes(5);
        var createdAt = LocalDateTime.now();
        var updatedAt = createdAt.plusMinutes(1);

        var evidence = IncidentEvidence.restore(
                "ev-1", "inc-1", "doc.pdf", "http://url", "pdf",
                100, false, null, uploadedAt, createdAt, updatedAt
        );

        assertEquals(uploadedAt, evidence.getUploadedAt());
        assertEquals(createdAt, evidence.getCreatedAt());
        assertEquals(updatedAt, evidence.getUpdatedAt());
    }

    @Test
    void restoreWithoutCreatedAtBackfillsUploadedAtFromUpdatedAt() {
        var updatedAt = LocalDateTime.now();

        var evidence = IncidentEvidence.restore(
                "ev-1", "inc-1", "doc.pdf", "http://url", "pdf",
                100, false, null, null, updatedAt
        );

        assertEquals(updatedAt, evidence.getUploadedAt());
        assertNull(evidence.getCreatedAt());
        assertEquals(updatedAt, evidence.getUpdatedAt());
    }

    /* == MARK AS DELETED TESTS == */

    @Test
    void markAsDeletedSetsDeletedAndDeletedAt() {
        var evidence = IncidentEvidence.create(
                "inc-1", "doc.pdf", "http://url", "pdf", 100
        );

        assertFalse(evidence.isDeleted());
        assertNull(evidence.getDeletedAt());

        evidence.markAsDeleted();

        assertTrue(evidence.isDeleted());
        assertNotNull(evidence.getDeletedAt());
    }

    @Test
    void markAsDeletedWhenAlreadyDeletedDoesNothing() {
        var now = LocalDateTime.now();
        var evidence = IncidentEvidence.restore(
                "ev-1", "inc-1", "doc.pdf", "http://url", "pdf",
                100, true, now, now, now
        );
        var deletedAtBefore = evidence.getDeletedAt();
        var updatedAtBefore = evidence.getUpdatedAt();
        evidence.clock = Clock.fixed(
                updatedAtBefore.plusSeconds(1).atZone(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()
        );

        evidence.markAsDeleted();

        assertTrue(evidence.isDeleted());
        assertEquals(deletedAtBefore, evidence.getDeletedAt());
        assertEquals(updatedAtBefore, evidence.getUpdatedAt());
    }

    @Test
    void markAsDeletedUpdatesUpdatedAt() {
        var evidence = IncidentEvidence.create(
                "inc-1", "doc.pdf", "http://url", "pdf", 100
        );
        var originalUpdatedAt = evidence.getUpdatedAt();
        evidence.clock = Clock.fixed(
                originalUpdatedAt.plusSeconds(1).atZone(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()
        );

        evidence.markAsDeleted();

        assertTrue(evidence.getUpdatedAt().isAfter(originalUpdatedAt));
    }

    @Test
    void canBeDeletedUsesUploadedAtInsteadOfCreatedAt() {
        var uploadedAt = LocalDateTime.of(2026, 6, 24, 12, 0);
        var createdAt = uploadedAt.minusHours(1);
        var updatedAt = uploadedAt.plusHours(1);
        var evidence = IncidentEvidence.restore(
                "ev-1", "inc-1", "doc.pdf", "http://url", "pdf",
                100, false, null, uploadedAt, createdAt, updatedAt
        );
        var clock = Clock.fixed(
                uploadedAt.plusMinutes(10).atZone(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()
        );

        assertTrue(evidence.canBeDeleted(clock));
    }

    /* == BELONGS TO TESTS == */

    @Test
    void belongsToReturnsTrueForMatchingIncidentId() {
        var evidence = IncidentEvidence.create(
                "inc-1", "doc.pdf", "http://url", "pdf", 100
        );

        assertTrue(evidence.belongsTo("inc-1"));
    }

    @Test
    void belongsToReturnsFalseForDifferentIncidentId() {
        var evidence = IncidentEvidence.create(
                "inc-1", "doc.pdf", "http://url", "pdf", 100
        );

        assertFalse(evidence.belongsTo("other-inc"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void belongsToWithBlankIncidentIdThrowsException(String incidentId) {
        var evidence = IncidentEvidence.create(
                "inc-1", "doc.pdf", "http://url", "pdf", 100
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> evidence.belongsTo(incidentId)
        );
    }

    /* == VALIDATION TESTS == */

    @Test
    void createTrimsWhitespaceFromIncidentId() {
        var evidence = IncidentEvidence.create(
                "  inc-1  ", "doc.pdf", "http://url", "pdf", 100
        );
        assertEquals("inc-1", evidence.getIncidentId());
    }

    @Test
    void createTrimsWhitespaceFromFileName() {
        var evidence = IncidentEvidence.create(
                "inc-1", "  doc.pdf  ", "http://url", "pdf", 100
        );
        assertEquals("doc.pdf", evidence.getFileName());
    }

    @Test
    void createTrimsWhitespaceFromFileUrl() {
        var evidence = IncidentEvidence.create(
                "inc-1", "doc.pdf", "  http://url  ", "pdf", 100
        );
        assertEquals("http://url", evidence.getFileUrl());
    }

    @Test
    void createTrimsWhitespaceFromMimeType() {
        var evidence = IncidentEvidence.create(
                "inc-1", "doc.pdf", "http://url", "  pdf  ", 100
        );
        assertEquals("pdf", evidence.getMimeType());
    }

}
