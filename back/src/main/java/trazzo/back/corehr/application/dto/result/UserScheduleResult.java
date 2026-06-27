package trazzo.back.corehr.application.dto.result;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record UserScheduleResult(
        Long id,
        Long tenantUserId,
        Long scheduleId,
        ShiftResult.ScheduleSummary schedule,
        String description,
        LocalTime entryTime,
        LocalTime departureTime,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
