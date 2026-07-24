package trazzo.back.incidents.application.dto.result;

import java.time.LocalDateTime;

public record IncidentEvidenceResult(
        String id,
        String incidenciaId,
        String fileName,
        String fileKey,
        String downloadUrl,
        String mimeType,
        int fileSize,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
