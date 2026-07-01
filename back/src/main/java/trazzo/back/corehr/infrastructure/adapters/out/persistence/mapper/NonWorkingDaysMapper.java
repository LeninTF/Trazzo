package trazzo.back.corehr.infrastructure.adapters.out.persistence.mapper;

import trazzo.back.corehr.domain.model.schedule.NonWorkingDays;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.entity.NonWorkingDaysEntity;

public final class NonWorkingDaysMapper {

    private NonWorkingDaysMapper() {
    }

    public static NonWorkingDaysEntity toEntity(NonWorkingDays domain) {
        var entity = new NonWorkingDaysEntity();
        entity.setId(domain.getId());
        entity.setDate(domain.getDate());
        entity.setDescription(domain.getDescription());
        entity.setRecurring(domain.isRecurring());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }

    public static NonWorkingDays toDomain(NonWorkingDaysEntity entity) {
        return NonWorkingDays.restore(
                entity.getId(),
                entity.getDate(),
                entity.getDescription(),
                entity.isRecurring(),
                entity.getCreatedAt()
        );
    }
}
