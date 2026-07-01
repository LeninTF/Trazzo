package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import trazzo.back.corehr.application.dto.result.AttendanceResult;
import trazzo.back.corehr.application.dto.result.PaginatedResult;

import java.util.List;

public record AttendanceListResponse(
        List<AttendanceResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        String scopeAplicado
) {
    public static AttendanceListResponse from(PaginatedResult<AttendanceResult> paginated, String scopeAplicado) {
        var content = paginated.content().stream()
                .map(AttendanceResponse::from)
                .toList();
        return new AttendanceListResponse(content, paginated.page(), paginated.size(),
                paginated.totalElements(), paginated.totalPages(), scopeAplicado);
    }
}
