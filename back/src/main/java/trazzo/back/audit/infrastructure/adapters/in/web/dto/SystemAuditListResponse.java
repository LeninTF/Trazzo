package trazzo.back.audit.infrastructure.adapters.in.web.dto;

import trazzo.back.audit.application.dto.result.PaginatedResult;
import trazzo.back.audit.application.dto.result.SystemAuditResult;
import java.util.List;

public record SystemAuditListResponse(
    List<SystemAuditResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
    public static SystemAuditListResponse from(PaginatedResult<SystemAuditResult> result) {
        var content = result.content().stream().map(SystemAuditResponse::from).toList();
        return new SystemAuditListResponse(content, result.page(), result.size(), result.totalElements(), result.totalPages());
    }
}
