package trazzo.back.corehr.domain.model.schedule;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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
    private List<DayOfWeek> daysOfWeek;

    private Schedule(
            Long id,
            Long shiftId,
            String name,
            String description,
            LocalTime entryTime,
            LocalTime departureTime,
            List<DayOfWeek> daysOfWeek,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        super(id, createdAt, updatedAt);
        this.shiftId = shiftId;
        this.name = DomainModelValidator.requireScheduleText(name, "name");
        this.description = DomainModelValidator.normalizeOptionalText(description);
        this.entryTime = DomainModelValidator.requireTime(entryTime, "entryTime");
        this.departureTime = DomainModelValidator.requireValidDepartureTime(entryTime, departureTime);
        this.daysOfWeek = normalizeDaysOfWeek(daysOfWeek);
    }

    public static Schedule create(Long shiftId, String name, String description,
                                   LocalTime entryTime, LocalTime departureTime,
                                   List<DayOfWeek> daysOfWeek) {
        LocalDateTime now = LocalDateTime.now();
        return new Schedule(null, shiftId, name, description, entryTime, departureTime,
                daysOfWeek, now, now);
    }

    public static Schedule restore(
            Long id, Long shiftId, String name, String description,
            LocalTime entryTime, LocalTime departureTime, List<DayOfWeek> daysOfWeek,
            LocalDateTime createdAt, LocalDateTime updatedAt
    ) {
        return new Schedule(id, shiftId, name, description, entryTime, departureTime,
                daysOfWeek, createdAt, updatedAt);
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

    public void updateDaysOfWeek(List<DayOfWeek> daysOfWeek) {
        this.daysOfWeek = normalizeDaysOfWeek(daysOfWeek);
        touch();
    }

    public List<DayOfWeek> getDaysOfWeek() {
        return Collections.unmodifiableList(this.daysOfWeek);
    }

    private static List<DayOfWeek> normalizeDaysOfWeek(List<DayOfWeek> daysOfWeek) {
        if (daysOfWeek == null || daysOfWeek.isEmpty()) {
            return Collections.emptyList();
        }
        Set<DayOfWeek> deduped = new LinkedHashSet<>(daysOfWeek);
        return Collections.unmodifiableList(new ArrayList<>(deduped));
    }
}
