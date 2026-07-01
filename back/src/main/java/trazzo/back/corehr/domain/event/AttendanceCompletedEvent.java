package trazzo.back.corehr.domain.event;

import java.time.LocalDateTime;
import trazzo.back.corehr.domain.model.AttendanceState;

public record AttendanceCompletedEvent(
        String attendanceId,
        Long tenantUserId,
        LocalDateTime checkOut,
        AttendanceState state,
        LocalDateTime occurredAt
) implements CoreHrDomainEvent {
}
