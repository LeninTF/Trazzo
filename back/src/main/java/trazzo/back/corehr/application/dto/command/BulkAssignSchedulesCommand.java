package trazzo.back.corehr.application.dto.command;

import java.time.LocalTime;

import java.util.List;

public record BulkAssignSchedulesCommand(
        List<Long> tenantUserIds,
        Long scheduleId,
        String description
) {
    public record Item(Long tenantUserId, String description, LocalTime entryTime, LocalTime departureTime) {
    }
}
