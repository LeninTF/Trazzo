package trazzo.back.corehr.domain.model.schedule;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import trazzo.back.corehr.domain.model.BaseDomainModel;
import trazzo.back.corehr.domain.model.DomainModelValidator;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSchedule extends BaseDomainModel {

    private Long tenantUserId;
    private Long scheduleId;
    private String description;
    private LocalTime entryTime;
    private LocalTime departureTime;

    private UserSchedule(
            Long id,
            Long tenantUserId,
            Long scheduleId,
            String description,
            LocalTime entryTime,
            LocalTime departureTime,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        super(id, createdAt, updatedAt);
        this.tenantUserId = DomainModelValidator.requireScheduleTenantUserId(tenantUserId);
        this.scheduleId = scheduleId;
        this.description = DomainModelValidator.normalizeOptionalText(description);
        this.entryTime = DomainModelValidator.requireTime(entryTime, "entryTime");
        this.departureTime = DomainModelValidator.requireValidDepartureTime(entryTime, departureTime);
    }

    public static UserSchedule create(Long tenantUserId, Long scheduleId, String description, LocalTime entryTime, LocalTime departureTime) {
        LocalDateTime now = LocalDateTime.now();
        return new UserSchedule(null, tenantUserId, scheduleId, description, entryTime, departureTime, now, now);
    }

    public static UserSchedule restore(
            Long id,
            Long tenantUserId,
            Long scheduleId,
            String description,
            LocalTime entryTime,
            LocalTime departureTime,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return new UserSchedule(id, tenantUserId, scheduleId, description, entryTime, departureTime, createdAt, updatedAt);
    }

    public void reschedule(LocalTime entryTime, LocalTime departureTime) {
        this.entryTime = DomainModelValidator.requireTime(entryTime, "entryTime");
        this.departureTime = DomainModelValidator.requireValidDepartureTime(entryTime, departureTime);
        touch();
    }

    public void updateDescription(String description) {
        this.description = DomainModelValidator.normalizeOptionalText(description);
        touch();
    }
}
