package trazzo.back.audit.infrastructure.adapters.in.web.dto;

import trazzo.back.audit.application.dto.result.PaginatedResult;
import trazzo.back.audit.application.dto.result.TenantSettingsRecordResult;
import java.util.List;

public record TenantSettingsRecordListResponse(
    List<TenantSettingsRecordResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
    public static TenantSettingsRecordListResponse from(PaginatedResult<TenantSettingsRecordResult> result) {
        var content = result.content().stream().map(TenantSettingsRecordResponse::from).toList();
        return new TenantSettingsRecordListResponse(content, result.page(), result.size(), result.totalElements(), result.totalPages());
    }
}
