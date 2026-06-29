package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTenantContactRequest(
        @NotNull @JsonProperty("tenant_user_id") Long tenantUserId,
        @NotBlank @Size(max = 50) String type
) {
}
