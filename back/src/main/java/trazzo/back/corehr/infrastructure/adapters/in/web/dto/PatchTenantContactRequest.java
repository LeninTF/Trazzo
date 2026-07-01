package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PatchTenantContactRequest(
        @NotBlank @Size(max = 50) String type
) {
}
