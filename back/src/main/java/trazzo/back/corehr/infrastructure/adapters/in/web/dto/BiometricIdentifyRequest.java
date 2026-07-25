package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record BiometricIdentifyRequest(
        @JsonProperty("event_type") String eventType,
        @NotBlank @JsonProperty("encrypted_template_base64") String encryptedTemplateBase64,
        @NotBlank @JsonProperty("encrypted_aes_key_base64") String encryptedAesKeyBase64,
        @NotBlank @JsonProperty("iv_base64") String ivBase64,
        @NotBlank @JsonProperty("tag_base64") String tagBase64,
        @NotNull @JsonProperty("captured_at_utc") LocalDateTime capturedAtUtc,
        @NotBlank @JsonProperty("device_code") String deviceCode,
        @JsonProperty("tenant_user_id") Long tenantUserId,
        @JsonProperty("offline_event_id") Integer offlineEventId,
        @JsonProperty("retry_count") Integer retryCount
) {
}
