package trazzo.back.incidents.domain.model;

import java.time.Clock;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IncidentEvidence {

    public static final int MAX_FILE_SIZE_BYTES = 15 * 1024 * 1024;

    private String id;
    private String incidentId;
    private String fileName;
    private String fileUrl;
    private String mimeType;
    private int fileSize;
    private boolean deleted;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    transient Clock clock = Clock.systemDefaultZone();

    private IncidentEvidence(
            String id,
            String incidentId,
            String fileName,
            String fileUrl,
            String mimeType,
            int fileSize,
            boolean deleted,
            LocalDateTime deletedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = normalizeOptionalId(id);
        this.incidentId = requireText(incidentId, "incidentId");
        this.fileName = requireText(fileName, "fileName");
        this.fileUrl = requireText(fileUrl, "fileUrl");
        this.mimeType = requireText(mimeType, "mimeType");
        this.fileSize = requireValidFileSize(fileSize);
        this.deleted = deleted;
        this.deletedAt = deletedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static IncidentEvidence create(
            String incidentId,
            String fileName,
            String fileUrl,
            String mimeType,
            int fileSize
    ) {
        LocalDateTime now = LocalDateTime.now();
        return new IncidentEvidence(
                null,
                incidentId,
                fileName,
                fileUrl,
                mimeType,
                fileSize,
                false,
                null,
                now,
                now
        );
    }

    public static IncidentEvidence restore(
            String id,
            String incidentId,
            String fileName,
            String fileUrl,
            String mimeType,
            int fileSize,
            boolean deleted,
            LocalDateTime deletedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return new IncidentEvidence(
                id,
                incidentId,
                fileName,
                fileUrl,
                mimeType,
                fileSize,
                deleted,
                deletedAt,
                createdAt,
                updatedAt
        );
    }

    public void markAsDeleted() {
        if (deleted) {
            return;
        }
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
        touch();
    }

    public boolean belongsTo(String incidentId) {
        return this.incidentId.equals(requireText(incidentId, "incidentId"));
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now(clock);
    }

    private static int requireValidFileSize(int fileSize) {
        if (fileSize <= 0) {
            throw new IllegalArgumentException("fileSize must be greater than zero");
        }
        if (fileSize > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("fileSize must not exceed 15MB");
        }
        return fileSize;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeOptionalId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
