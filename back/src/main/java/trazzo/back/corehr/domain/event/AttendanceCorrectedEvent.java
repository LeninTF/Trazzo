package trazzo.back.corehr.domain.event;

import java.time.LocalDateTime;
import trazzo.back.corehr.domain.model.AttendanceState;

public record AttendanceCorrectedEvent(
        String attendanceId,
        Long tenantUserId,
        AttendanceState previousState,
        AttendanceState newState,
        int previousMinutesLate,
        int newMinutesLate,
        LocalDateTime occurredAt
) implements CoreHrDomainEvent {
}
