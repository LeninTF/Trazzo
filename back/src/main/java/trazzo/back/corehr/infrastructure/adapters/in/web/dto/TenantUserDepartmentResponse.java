package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import trazzo.back.corehr.application.dto.result.TenantUserDepartmentResult;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TenantUserDepartmentResponse(
        Long id,
        @JsonProperty("tenant_user_id") Long tenantUserId,
        @JsonProperty("department_id") Long departmentId,
        @JsonProperty("department_name") String departmentName,
        @JsonProperty("is_primary") boolean isPrimary,
        @JsonProperty("start_date") LocalDate startDate,
        @JsonProperty("end_date") LocalDate endDate,
        @JsonProperty("created_at") LocalDateTime createdAt,
        @JsonProperty("updated_at") LocalDateTime updatedAt
) {
    public static TenantUserDepartmentResponse from(TenantUserDepartmentResult result) {
        return new TenantUserDepartmentResponse(result.id(), result.tenantUserId(),
                result.departmentId(), result.departmentName(), result.isPrimary(),
                result.startDate(), result.endDate(), result.createdAt(), result.updatedAt());
    }
}
