package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public record PatchTenantUserDepartmentRequest(
        @JsonProperty("end_date") LocalDate endDate,
        @JsonProperty("is_primary") Boolean isPrimary
) {
}
