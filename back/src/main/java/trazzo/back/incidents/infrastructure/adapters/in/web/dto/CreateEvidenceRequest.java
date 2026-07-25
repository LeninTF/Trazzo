package trazzo.back.incidents.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record CreateEvidenceRequest(
        @NotBlank @JsonProperty("file_name") String fileName,
        @NotBlank @JsonProperty("file_key") String fileKey,
        @NotBlank
        @Pattern(
                regexp = "^(application/pdf|image/png|image/jpeg|application/msword|application/vnd\\.openxmlformats-officedocument\\.wordprocessingml\\.document|video/mp4|video/quicktime)$",
                message = "Tipo MIME no permitido. Formatos válidos: PDF, PNG, JPEG, DOC, DOCX, MP4, QuickTime."
        )
        @JsonProperty("mime_type") String mimeType,
        @Positive @JsonProperty("file_size") int fileSize
) {
}
