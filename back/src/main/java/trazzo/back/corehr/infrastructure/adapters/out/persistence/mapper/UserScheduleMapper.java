package trazzo.back.corehr.infrastructure.adapters.out.persistence.mapper;

import trazzo.back.corehr.domain.model.schedule.UserSchedule;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.entity.UserScheduleEntity;

public final class UserScheduleMapper {

    private UserScheduleMapper() {
    }

    public static UserScheduleEntity toEntity(UserSchedule domain) {
        var entity = new UserScheduleEntity();
        entity.setId(domain.getId());
        entity.setTenantUserId(domain.getTenantUserId());
        entity.setScheduleId(domain.getScheduleId());
        entity.setDescription(domain.getDescription());
        entity.setEntryTime(domain.getEntryTime());
        entity.setDepartureTime(domain.getDepartureTime());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }

    public static UserSchedule toDomain(UserScheduleEntity entity) {
        return UserSchedule.restore(
                entity.getId(),
                entity.getTenantUserId(),
                entity.getScheduleId(),
                entity.getDescription(),
                entity.getEntryTime(),
                entity.getDepartureTime(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
