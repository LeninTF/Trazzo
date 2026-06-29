package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import trazzo.back.corehr.application.dto.result.PaginatedResult;
import trazzo.back.corehr.application.dto.result.UserBiometriaResult;

import java.util.List;

public record UserBiometriaListResponse(
        List<UserBiometriaResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static UserBiometriaListResponse from(PaginatedResult<UserBiometriaResult> paginated) {
        var content = paginated.content().stream()
                .map(UserBiometriaResponse::from)
                .toList();
        return new UserBiometriaListResponse(content, paginated.page(), paginated.size(),
                paginated.totalElements(), paginated.totalPages());
    }
}
