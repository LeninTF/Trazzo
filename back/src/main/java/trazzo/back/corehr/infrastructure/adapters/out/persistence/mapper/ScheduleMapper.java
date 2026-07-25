package trazzo.back.corehr.infrastructure.adapters.out.persistence.mapper;

import trazzo.back.corehr.domain.model.schedule.Schedule;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.entity.ScheduleDayEntity;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.entity.ScheduleEntity;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class ScheduleMapper {

    private ScheduleMapper() {
    }

    public static ScheduleEntity toEntity(Schedule domain) {
        var entity = new ScheduleEntity();
        entity.setId(domain.getId());
        entity.setShiftId(domain.getShiftId());
        entity.setName(domain.getName());
        entity.setDescription(domain.getDescription());
        entity.setEntryTime(domain.getEntryTime());
        entity.setDepartureTime(domain.getDepartureTime());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        var existingDays = entity.getDaysOfWeek() != null ? entity.getDaysOfWeek() : new ArrayList<ScheduleDayEntity>();
        applyDaysOfWeek(entity, domain.getDaysOfWeek(), existingDays);
        return entity;
    }

    public static Schedule toDomain(ScheduleEntity entity) {
        var days = entity.getDaysOfWeek() == null
                ? List.<DayOfWeek>of()
                : entity.getDaysOfWeek().stream()
                        .map(ScheduleMapper::toDayOfWeek)
                        .toList();
        return Schedule.restore(
                entity.getId(),
                entity.getShiftId(),
                entity.getName(),
                entity.getDescription(),
                entity.getEntryTime(),
                entity.getDepartureTime(),
                days,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private static void applyDaysOfWeek(ScheduleEntity entity, List<DayOfWeek> domainDays,
                                        List<ScheduleDayEntity> existingDays) {
        var byName = existingDays.stream()
                .collect(Collectors.toMap(ScheduleDayEntity::getDayOfWeek, d -> d));
        entity.getDaysOfWeek().clear();
        if (domainDays == null) {
            return;
        }
        for (DayOfWeek day : domainDays) {
            var code = day.name();
            var existing = byName.get(code);
            if (existing != null) {
                entity.getDaysOfWeek().add(existing);
            } else {
                var newDay = new ScheduleDayEntity();
                newDay.setDayOfWeek(code);
                entity.getDaysOfWeek().add(newDay);
            }
        }
    }

    private static DayOfWeek toDayOfWeek(ScheduleDayEntity entity) {
        return DayOfWeek.valueOf(entity.getDayOfWeek());
    }
}
