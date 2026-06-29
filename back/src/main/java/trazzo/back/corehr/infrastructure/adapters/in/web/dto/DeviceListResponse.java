package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import trazzo.back.corehr.application.dto.result.DeviceResult;
import trazzo.back.corehr.application.dto.result.PaginatedResult;

import java.util.List;

public record DeviceListResponse(
        List<DeviceResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static DeviceListResponse from(PaginatedResult<DeviceResult> paginated) {
        var content = paginated.content().stream()
                .map(DeviceResponse::from)
                .toList();
        return new DeviceListResponse(content, paginated.page(), paginated.size(),
                paginated.totalElements(), paginated.totalPages());
    }
}
