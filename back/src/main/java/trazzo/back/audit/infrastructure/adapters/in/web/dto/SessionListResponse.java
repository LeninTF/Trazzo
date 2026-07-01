package trazzo.back.audit.infrastructure.adapters.in.web.dto;

import trazzo.back.audit.application.dto.result.PaginatedResult;
import trazzo.back.audit.application.dto.result.SessionResult;
import java.util.List;

public record SessionListResponse(
    List<SessionResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
    public static SessionListResponse from(PaginatedResult<SessionResult> result) {
        var content = result.content().stream().map(SessionResponse::from).toList();
        return new SessionListResponse(content, result.page(), result.size(), result.totalElements(), result.totalPages());
    }
}
