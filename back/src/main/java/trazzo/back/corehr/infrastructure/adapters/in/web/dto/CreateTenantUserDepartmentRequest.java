package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateTenantUserDepartmentRequest(
        @NotNull @JsonProperty("department_id") Long departmentId,
        @JsonProperty("is_primary") boolean isPrimary,
        @NotNull @JsonProperty("start_date") LocalDate startDate,
        @JsonProperty("end_date") LocalDate endDate
) {
}
