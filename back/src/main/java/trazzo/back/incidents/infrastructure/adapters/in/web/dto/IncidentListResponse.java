package trazzo.back.incidents.infrastructure.adapters.in.web.dto;

import trazzo.back.incidents.application.dto.result.IncidentResult;
import trazzo.back.incidents.application.dto.result.PaginatedResult;

import java.util.List;

public record IncidentListResponse(
        List<IncidentResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        String scopeAplicado
) {
    public static IncidentListResponse from(PaginatedResult<IncidentResult> paginated, String scopeAplicado) {
        var content = paginated.content().stream()
                .map(IncidentResponse::from)
                .toList();
        return new IncidentListResponse(content, paginated.page(), paginated.size(),
                paginated.totalElements(), paginated.totalPages(), scopeAplicado);
    }
}
