package trazzo.back.audit.infrastructure.adapters.in.web.dto;

import trazzo.back.audit.application.dto.result.AuditLogResult;
import trazzo.back.audit.application.dto.result.PaginatedResult;
import java.util.List;

public record AuditLogListResponse(
    List<AuditLogResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
    public static AuditLogListResponse from(PaginatedResult<AuditLogResult> result) {
        var content = result.content().stream().map(AuditLogResponse::from).toList();
        return new AuditLogListResponse(content, result.page(), result.size(), result.totalElements(), result.totalPages());
    }
}
