package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import trazzo.back.corehr.application.dto.result.PaginatedResult;
import trazzo.back.corehr.application.dto.result.ToleranciaResult;

import java.util.List;

public record ToleranciaListResponse(
        List<ToleranciaResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static ToleranciaListResponse from(PaginatedResult<ToleranciaResult> paginated) {
        var content = paginated.content().stream()
                .map(ToleranciaResponse::from)
                .toList();
        return new ToleranciaListResponse(content, paginated.page(), paginated.size(),
                paginated.totalElements(), paginated.totalPages());
    }
}
