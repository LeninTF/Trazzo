package trazzo.back.corehr.application.dto.result;

import java.util.List;

public record BulkAssignSchedulesResult(
        Long scheduleId,
        List<ItemResult> items,
        int total,
        int created,
        int skipped,
        int errors
) {
    public record ItemResult(Long tenantUserId, String status, String message) {
    }
}
