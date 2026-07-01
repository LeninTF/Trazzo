package trazzo.back.incidents.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CreateEvidenceRequest(
        @NotBlank @JsonProperty("file_name") String fileName,
        @NotBlank @JsonProperty("file_url") String fileUrl,
        @NotBlank @JsonProperty("mime_type") String mimeType,
        @Positive @JsonProperty("file_size") int fileSize
) {
}
