package trazzo.back.corehr.domain.model.schedule;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import trazzo.back.corehr.domain.exception.InvalidScheduleException;
import trazzo.back.corehr.domain.specification.ScheduleTimeSpec;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSchedule {

    private Long id;
    private Long tenantUserId;
    private Long scheduleId;
    private String description;
    private LocalTime entryTime;
    private LocalTime departureTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    transient Clock clock = Clock.systemDefaultZone();

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
        this.id = id;
        this.tenantUserId = requireTenantUserId(tenantUserId);
        this.scheduleId = scheduleId;
        this.description = normalizeOptionalText(description);
        this.entryTime = requireTime(entryTime, "entryTime");
        this.departureTime = requireValidDepartureTime(entryTime, departureTime);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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
        this.entryTime = requireTime(entryTime, "entryTime");
        this.departureTime = requireValidDepartureTime(entryTime, departureTime);
        touch();
    }

    public void updateDescription(String description) {
        this.description = normalizeOptionalText(description);
        touch();
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now(clock);
    }

    private static Long requireTenantUserId(Long tenantUserId) {
        if (tenantUserId == null) {
            throw new InvalidScheduleException("tenantUserId is required");
        }
        return tenantUserId;
    }

    private static LocalTime requireTime(LocalTime value, String fieldName) {
        if (value == null) {
            throw new InvalidScheduleException(fieldName + " is required");
        }
        return value;
    }

    private static LocalTime requireValidDepartureTime(LocalTime entryTime, LocalTime departureTime) {
        if (departureTime == null) {
            throw new InvalidScheduleException("departureTime is required");
        }
        if (!new ScheduleTimeSpec().isValidScheduleTime(entryTime, departureTime)) {
            throw new InvalidScheduleException("departureTime must be after entryTime");
        }
        return departureTime;
    }

    private static String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
