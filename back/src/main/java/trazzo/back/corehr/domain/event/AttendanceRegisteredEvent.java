package trazzo.back.corehr.domain.event;

import java.time.LocalDateTime;

public record AttendanceRegisteredEvent(
        String attendanceId,
        Long tenantUserId,
        Long scheduleId,
        Long deviceId,
        LocalDateTime occurredAt
) implements CoreHrDomainEvent {
}
