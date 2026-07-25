package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import trazzo.back.corehr.application.dto.result.BulkAssignSchedulesResult;

import java.util.List;

public record BulkAssignUserSchedulesResponse(
        @JsonProperty("schedule_id") Long scheduleId,
        List<ItemResponse> items,
        int total,
        int created,
        int skipped,
        int errors
) {
    public static BulkAssignUserSchedulesResponse from(BulkAssignSchedulesResult result) {
        var items = result.items().stream()
                .map(i -> new ItemResponse(i.tenantUserId(), i.status(), i.message()))
                .toList();
        return new BulkAssignUserSchedulesResponse(result.scheduleId(), items,
                result.total(), result.created(), result.skipped(), result.errors());
    }

    public record ItemResponse(
            @JsonProperty("tenant_user_id") Long tenantUserId,
            String status,
            String message
    ) {
    }
}
