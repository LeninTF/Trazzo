package trazzo.back.incidents.infrastructure.adapters.out.persistence.mapper;

import trazzo.back.incidents.domain.model.IncidentType;
import trazzo.back.incidents.infrastructure.adapters.out.persistence.entity.IncidentTypeEntity;

public final class IncidentTypeMapper {

    private IncidentTypeMapper() {
    }

    public static IncidentTypeEntity toEntity(IncidentType domain) {
        var entity = new IncidentTypeEntity();
        entity.setId(domain.getId());
        entity.setNombre(domain.getNombre());
        entity.setDescripcion(domain.getDescripcion());
        entity.setActivo(domain.isActivo());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }

    public static IncidentType toDomain(IncidentTypeEntity entity) {
        return IncidentType.restore(
                entity.getId(),
                entity.getNombre(),
                entity.getDescripcion(),
                entity.isActivo(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
