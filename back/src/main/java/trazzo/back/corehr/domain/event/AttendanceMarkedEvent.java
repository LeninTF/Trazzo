package trazzo.back.corehr.domain.event;

import java.time.LocalDateTime;
import trazzo.back.corehr.domain.model.AttendanceState;

public record AttendanceMarkedEvent(
        String attendanceId,
        Long tenantUserId,
        Long scheduleId,
        Long deviceId,
        AttendanceState state,
        int minutesLate,
        LocalDateTime occurredAt
) implements CoreHrDomainEvent {
}
