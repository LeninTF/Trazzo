package trazzo.back.incidents.infrastructure.adapters.in.web.dto;

import trazzo.back.incidents.application.dto.result.IncidentTypeResult;
import trazzo.back.incidents.application.dto.result.PaginatedResult;

import java.util.List;

public record IncidentTypeListResponse(
        List<IncidentTypeResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static IncidentTypeListResponse from(PaginatedResult<IncidentTypeResult> paginated) {
        var content = paginated.content().stream()
                .map(IncidentTypeResponse::from)
                .toList();
        return new IncidentTypeListResponse(content, paginated.page(), paginated.size(),
                paginated.totalElements(), paginated.totalPages());
    }
}
