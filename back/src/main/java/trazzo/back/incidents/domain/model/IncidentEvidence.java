package trazzo.back.incidents.domain.model;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import trazzo.back.incidents.domain.exception.InvalidIncidentEvidenceException;
import trazzo.back.incidents.domain.specification.EvidenceDeletionWindowSpec;
import trazzo.back.incidents.domain.specification.IncidentEvidenceSpec;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IncidentEvidence {

    public static final int MAX_FILE_SIZE_BYTES = IncidentEvidenceSpec.MAX_FILE_SIZE_BYTES;

    private String id;
    private String incidentId;
    private String fileName;
    private String fileKey;
    private String mimeType;
    private int fileSize;
    private boolean deleted;
    private LocalDateTime deletedAt;
    private LocalDateTime uploadedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    transient Clock clock = Clock.systemDefaultZone();

    private IncidentEvidence(
            String id,
            String incidentId,
            String fileName,
            String fileKey,
            String mimeType,
            int fileSize,
            boolean deleted,
            LocalDateTime deletedAt,
            LocalDateTime uploadedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = normalizeOptionalId(id);
        this.incidentId = requireText(incidentId, "incidentId");
        this.fileName = requireText(fileName, "fileName");
        this.fileKey = requireText(fileKey, "fileKey");
        this.mimeType = requireText(mimeType, "mimeType");
        this.fileSize = requireValidFileSize(fileSize);
        this.deleted = deleted;
        this.deletedAt = deletedAt;
        this.uploadedAt = requireUploadTimestamp(uploadedAt, createdAt, updatedAt, deletedAt);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static IncidentEvidence create(
            String incidentId,
            String fileName,
            String fileKey,
            String mimeType,
            int fileSize
    ) {
        LocalDateTime now = LocalDateTime.now();
        return new IncidentEvidence(
                UUID.randomUUID().toString(),
                incidentId,
                fileName,
                fileKey,
                mimeType,
                fileSize,
                false,
                null,
                now,
                now,
                now
        );
    }

    public static IncidentEvidence restore(
            String id,
            String incidentId,
            String fileName,
            String fileKey,
            String mimeType,
            int fileSize,
            boolean deleted,
            LocalDateTime deletedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return restore(
                id,
                incidentId,
                fileName,
                fileKey,
                mimeType,
                fileSize,
                deleted,
                deletedAt,
                createdAt,
                createdAt,
                updatedAt
        );
    }

    public static IncidentEvidence restore(
            String id,
            String incidentId,
            String fileName,
            String fileKey,
            String mimeType,
            int fileSize,
            boolean deleted,
            LocalDateTime deletedAt,
            LocalDateTime uploadedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return new IncidentEvidence(
                id,
                incidentId,
                fileName,
                fileKey,
                mimeType,
                fileSize,
                deleted,
                deletedAt,
                uploadedAt,
                createdAt,
                updatedAt
        );
    }

    public void markAsDeleted() {
        if (deleted) {
            return;
        }
        this.deleted = true;
        this.deletedAt = LocalDateTime.now(clock);
        touch();
    }

    public boolean canBeDeleted(Clock clock) {
        return !deleted && new EvidenceDeletionWindowSpec().isSatisfiedBy(this, clock);
    }

    public boolean belongsTo(String incidentId) {
        return this.incidentId.equals(requireText(incidentId, "incidentId"));
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now(clock);
    }

    private static int requireValidFileSize(int fileSize) {
        if (!new IncidentEvidenceSpec().isValidFileSize(fileSize)) {
            throw new InvalidIncidentEvidenceException("fileSize must be greater than zero and must not exceed 15MB");
        }
        return fileSize;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new InvalidIncidentEvidenceException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeOptionalId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static LocalDateTime requireUploadTimestamp(
            LocalDateTime uploadedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime deletedAt
    ) {
        if (uploadedAt != null) {
            return uploadedAt;
        }
        if (createdAt != null) {
            return createdAt;
        }
        if (updatedAt != null) {
            return updatedAt;
        }
        if (deletedAt != null) {
            return deletedAt;
        }
        return LocalDateTime.now();
    }
}
