package trazzo.back.corehr.infrastructure.adapters.out.persistence.mapper;

import trazzo.back.corehr.domain.model.schedule.Tolerancia;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.entity.ToleranciaEntity;

public final class ToleranciaMapper {

    private ToleranciaMapper() {
    }

    public static ToleranciaEntity toEntity(Tolerancia domain) {
        var entity = new ToleranciaEntity();
        entity.setId(domain.getId());
        entity.setScheduleId(domain.getScheduleId());
        entity.setName(domain.getName());
        entity.setType(domain.getType());
        entity.setMinutes(domain.getMinutes());
        entity.setDescription(domain.getDescription());
        entity.setActivo(domain.isActivo());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }

    public static Tolerancia toDomain(ToleranciaEntity entity) {
        return Tolerancia.restore(
                entity.getId(),
                entity.getScheduleId(),
                entity.getName(),
                entity.getType(),
                entity.getMinutes(),
                entity.getDescription(),
                entity.isActivo(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
