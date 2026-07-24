package trazzo.back.incidents.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import trazzo.back.incidents.application.dto.result.IncidentEvidenceResult;

import java.time.LocalDateTime;

public record IncidentEvidenceResponse(
        String id,
        @JsonProperty("incidencia_id") String incidenciaId,
        @JsonProperty("file_name") String fileName,
        @JsonProperty("download_url") String downloadUrl,
        @JsonProperty("file_key") String fileKey,
        @JsonProperty("mime_type") String mimeType,
        @JsonProperty("file_size") int fileSize,
        @JsonProperty("created_at") LocalDateTime createdAt,
        @JsonProperty("updated_at") LocalDateTime updatedAt
) {
    public static IncidentEvidenceResponse from(IncidentEvidenceResult result) {
        return new IncidentEvidenceResponse(result.id(), result.incidenciaId(), result.fileName(),
                result.downloadUrl(), result.fileKey(), result.mimeType(), result.fileSize(), result.createdAt(), result.updatedAt());
    }
}
