package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import trazzo.back.corehr.application.dto.result.TenantUserDepartmentResult;

import java.util.List;

public record TenantUserDepartmentListResponse(
        List<TenantUserDepartmentResponse> content
) {
    public static TenantUserDepartmentListResponse from(List<TenantUserDepartmentResult> results) {
        var content = results.stream()
                .map(TenantUserDepartmentResponse::from)
                .toList();
        return new TenantUserDepartmentListResponse(content);
    }
}
