package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import trazzo.back.corehr.application.dto.result.PaginatedResult;
import trazzo.back.corehr.application.dto.result.TenantContactResult;

import java.util.List;

public record TenantContactListResponse(
        List<TenantContactResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static TenantContactListResponse from(PaginatedResult<TenantContactResult> paginated) {
        var content = paginated.content().stream()
                .map(TenantContactResponse::from)
                .toList();
        return new TenantContactListResponse(content, paginated.page(), paginated.size(),
                paginated.totalElements(), paginated.totalPages());
    }
}
