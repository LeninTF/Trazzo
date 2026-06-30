package trazzo.back.audit.infrastructure.adapters.in.web.dto;

import trazzo.back.audit.application.dto.result.LogInHistoryResult;
import trazzo.back.audit.application.dto.result.PaginatedResult;
import java.util.List;

public record LogInHistoryListResponse(
    List<LogInHistoryResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
    public static LogInHistoryListResponse from(PaginatedResult<LogInHistoryResult> result) {
        var content = result.content().stream().map(LogInHistoryResponse::from).toList();
        return new LogInHistoryListResponse(content, result.page(), result.size(), result.totalElements(), result.totalPages());
    }
}
