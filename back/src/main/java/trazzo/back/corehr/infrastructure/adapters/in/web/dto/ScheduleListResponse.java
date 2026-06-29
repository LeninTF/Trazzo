package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import trazzo.back.corehr.application.dto.result.PaginatedResult;
import trazzo.back.corehr.application.dto.result.ScheduleResult;

import java.util.List;

public record ScheduleListResponse(
        List<ScheduleResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static ScheduleListResponse from(PaginatedResult<ScheduleResult> paginated) {
        var content = paginated.content().stream()
                .map(ScheduleResponse::from)
                .toList();
        return new ScheduleListResponse(content, paginated.page(), paginated.size(),
                paginated.totalElements(), paginated.totalPages());
    }
}
