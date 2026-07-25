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
        LocalDateTime updatedAt,
        TenantUserSummary tenantUser
) {
    public UserScheduleResult(Long id, Long tenantUserId, Long scheduleId, ShiftResult.ScheduleSummary schedule,
                             String description, LocalTime entryTime, LocalTime departureTime,
                             LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(id, tenantUserId, scheduleId, schedule, description, entryTime, departureTime,
                createdAt, updatedAt, null);
    }

    public record TenantUserSummary(
            Long id,
            String name,
            String fatherSurname,
            String motherSurname,
            String sede,
            String area,
            String department
    ) {
    }
}
