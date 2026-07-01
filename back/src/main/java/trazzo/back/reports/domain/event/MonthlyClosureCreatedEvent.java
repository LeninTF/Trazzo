package trazzo.back.reports.domain.event;

import lombok.Getter;
import trazzo.back.reports.domain.model.closure.ClosurePeriod;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Getter
public class MonthlyClosureCreatedEvent implements DomainEvent {

    private final UUID closureId;
    private final ClosurePeriod period;
    private final UUID createdByUserId;
    private final LocalDateTime createdAt;

    public MonthlyClosureCreatedEvent(UUID closureId, ClosurePeriod period, UUID createdByUserId, LocalDateTime createdAt) {
        this.closureId = Objects.requireNonNull(closureId, "Closure id cannot be null");
        this.period = Objects.requireNonNull(period, "Period cannot be null");
        this.createdByUserId = Objects.requireNonNull(createdByUserId, "Created by user id cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "Created at cannot be null");
    }
}
