package trazzo.back.saasglobal.infrastructure.adapters.in.web.dto;

import java.util.List;
import trazzo.back.saasglobal.application.dto.result.PaginatedResult;
import trazzo.back.saasglobal.application.dto.result.SaasUserResult;

public record SaasUserListResponse(
        List<SaasUserProfileResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static SaasUserListResponse from(PaginatedResult<SaasUserResult> result) {
        List<SaasUserProfileResponse> content = result.content().stream()
                .map(SaasUserProfileResponse::from)
                .toList();
        return new SaasUserListResponse(content, result.page(), result.size(),
                result.totalElements(), result.totalPages());
    }
}
