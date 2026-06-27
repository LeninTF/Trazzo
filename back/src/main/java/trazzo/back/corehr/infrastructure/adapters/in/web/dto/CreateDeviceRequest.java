package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateDeviceRequest(
        @NotBlank String code,
        String name,
        @NotNull @JsonProperty("branch_id") Long branchId,
        String ip,
        Integer puerto,
        String ubicacion
) {
}
