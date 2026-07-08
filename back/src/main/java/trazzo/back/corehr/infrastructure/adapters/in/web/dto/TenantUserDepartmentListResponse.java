package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import trazzo.back.corehr.application.dto.result.PaginatedResult;
import trazzo.back.corehr.application.dto.result.TenantUserDepartmentResult;

import java.util.List;

public record TenantUserDepartmentListResponse(
        List<TenantUserDepartmentResponse> content,
        int page,
        int size,
        @JsonProperty("total_elements") long totalElements,
        @JsonProperty("total_pages") int totalPages
) {
    public static TenantUserDepartmentListResponse from(PaginatedResult<TenantUserDepartmentResult> result) {
        var content = result.content().stream()
                .map(TenantUserDepartmentResponse::from)
                .toList();
        return new TenantUserDepartmentListResponse(content, result.page(), result.size(), result.totalElements(), result.totalPages());
    }
}
