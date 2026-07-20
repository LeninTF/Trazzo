package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CompleteEnrollHttpRequest(
        @NotBlank @JsonProperty("enroll_token") String enrollToken,
        @NotBlank @JsonProperty("device_code") String deviceCode,
        @NotNull @JsonProperty("finger_index") Integer fingerIndex,
        @NotBlank @JsonProperty("encrypted_template_base64") String encryptedTemplateBase64,
        @NotBlank @JsonProperty("encrypted_aes_key_base64") String encryptedAesKeyBase64,
        @JsonProperty("iv_base64") String ivBase64,
        @JsonProperty("tag_base64") String tagBase64,
        @JsonProperty("captured_at_utc") LocalDateTime capturedAtUtc
) {
}