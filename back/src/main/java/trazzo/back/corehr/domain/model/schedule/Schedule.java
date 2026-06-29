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
public class Schedule extends BaseDomainModel {

    private Long shiftId;
    private String name;
    private String description;
    private LocalTime entryTime;
    private LocalTime departureTime;

    private Schedule(
            Long id,
            Long shiftId,
            String name,
            String description,
            LocalTime entryTime,
            LocalTime departureTime,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        super(id, createdAt, updatedAt);
        this.shiftId = shiftId;
        this.name = DomainModelValidator.requireScheduleText(name, "name");
        this.description = DomainModelValidator.normalizeOptionalText(description);
        this.entryTime = DomainModelValidator.requireTime(entryTime, "entryTime");
        this.departureTime = DomainModelValidator.requireValidDepartureTime(entryTime, departureTime);
    }

    public static Schedule create(Long shiftId, String name, String description, LocalTime entryTime, LocalTime departureTime) {
        LocalDateTime now = LocalDateTime.now();
        return new Schedule(null, shiftId, name, description, entryTime, departureTime, now, now);
    }

    public static Schedule restore(
            Long id,
            Long shiftId,
            String name,
            String description,
            LocalTime entryTime,
            LocalTime departureTime,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return new Schedule(id, shiftId, name, description, entryTime, departureTime, createdAt, updatedAt);
    }

    public void rename(String name) {
        this.name = DomainModelValidator.requireScheduleText(name, "name");
        touch();
    }

    public void updateDescription(String description) {
        this.description = DomainModelValidator.normalizeOptionalText(description);
        touch();
    }

    public void reschedule(LocalTime entryTime, LocalTime departureTime) {
        this.entryTime = DomainModelValidator.requireTime(entryTime, "entryTime");
        this.departureTime = DomainModelValidator.requireValidDepartureTime(entryTime, departureTime);
        touch();
    }
}
