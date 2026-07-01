package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import trazzo.back.corehr.application.dto.result.PaginatedResult;
import trazzo.back.corehr.application.dto.result.ShiftResult;

import java.util.List;

public record ShiftListResponse(
        List<ShiftResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static ShiftListResponse from(PaginatedResult<ShiftResult> paginated) {
        var content = paginated.content().stream()
                .map(ShiftResponse::from)
                .toList();
        return new ShiftListResponse(content, paginated.page(), paginated.size(),
                paginated.totalElements(), paginated.totalPages());
    }
}
