package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record InitEnrollRequest(
        @NotNull @JsonProperty("tenant_user_id") Long tenantUserId,
        @NotNull @JsonProperty("device_id") Long deviceId,
        @NotNull @Min(0) @Max(9) @JsonProperty("finger_index") Integer fingerIndex
) {
}
