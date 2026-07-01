package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import trazzo.back.corehr.application.dto.result.PaginatedResult;
import trazzo.back.corehr.application.dto.result.UserScheduleResult;

import java.util.List;

public record UserScheduleListResponse(
        List<UserScheduleResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static UserScheduleListResponse from(PaginatedResult<UserScheduleResult> paginated) {
        var content = paginated.content().stream()
                .map(UserScheduleResponse::from)
                .toList();
        return new UserScheduleListResponse(content, paginated.page(), paginated.size(),
                paginated.totalElements(), paginated.totalPages());
    }
}
