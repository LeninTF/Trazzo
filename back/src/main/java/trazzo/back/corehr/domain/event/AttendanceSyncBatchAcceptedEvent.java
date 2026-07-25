package trazzo.back.corehr.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record AttendanceSyncBatchAcceptedEvent(
        UUID correlationId,
        int acceptedCount,
        Long tenantUserId,
        LocalDateTime occurredAt
) implements CoreHrDomainEvent {
}
