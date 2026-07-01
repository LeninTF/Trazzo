package trazzo.back.corehr.infrastructure.adapters.out.persistence.mapper;

import trazzo.back.corehr.domain.model.schedule.Schedule;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.entity.ScheduleEntity;

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
        return entity;
    }

    public static Schedule toDomain(ScheduleEntity entity) {
        return Schedule.restore(
                entity.getId(),
                entity.getShiftId(),
                entity.getName(),
                entity.getDescription(),
                entity.getEntryTime(),
                entity.getDepartureTime(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
