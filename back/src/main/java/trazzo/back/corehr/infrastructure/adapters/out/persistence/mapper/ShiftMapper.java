package trazzo.back.corehr.infrastructure.adapters.out.persistence.mapper;

import trazzo.back.corehr.domain.model.schedule.Shift;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.entity.ShiftEntity;

public final class ShiftMapper {

    private ShiftMapper() {
    }

    public static ShiftEntity toEntity(Shift domain) {
        var entity = new ShiftEntity();
        entity.setId(domain.getId());
        entity.setName(domain.getName());
        entity.setDescription(domain.getDescription());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }

    public static Shift toDomain(ShiftEntity entity) {
        return Shift.restore(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
