package trazzo.back.incidents.infrastructure.adapters.out.persistence.mapper;

import trazzo.back.incidents.domain.model.IncidentType;
import trazzo.back.incidents.infrastructure.adapters.out.persistence.entity.IncidentTypeEntity;

public final class IncidentTypeMapper {

    private IncidentTypeMapper() {
    }

    public static IncidentTypeEntity toEntity(IncidentType domain) {
        return new IncidentTypeEntity(
                domain.getId(),
                domain.getNombre(),
                domain.getDescripcion(),
                domain.isActivo(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
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
