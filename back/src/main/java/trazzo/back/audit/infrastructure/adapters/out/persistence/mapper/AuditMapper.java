package trazzo.back.audit.infrastructure.adapters.out.persistence.mapper;

import trazzo.back.audit.domain.model.master.Audit;
import trazzo.back.audit.infrastructure.adapters.out.persistence.entity.AuditEntity;
import trazzo.back.audit.infrastructure.adapters.out.persistence.util.JsonUtils;

import java.util.Map;
import java.util.UUID;

public final class AuditMapper {

    private AuditMapper() {
    }

    public static AuditEntity toEntity(Audit domain) {
        var entity = new AuditEntity();
        if (domain.getId() != null) {
            entity.setId(UUID.fromString(domain.getId()));
        } else {
            entity.setId(UUID.randomUUID());
        }
        entity.setEntity(domain.getEntity());
        entity.setEntityId(domain.getEntityId());
        entity.setAction(domain.getAction());
        entity.setUserId(domain.getUserId() != null ? UUID.fromString(domain.getUserId()) : null);
        entity.setEndpoint(domain.getEndpoint());
        entity.setIpAddress(domain.getIpAddress());
        entity.setUserAgent(domain.getUserAgent());
        entity.setOldValue(serializeJson(domain.getPreviousValue()));
        entity.setNewValue(serializeJson(domain.getNewValue()));
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }

    public static Audit toDomain(AuditEntity entity) {
        return Audit.restore(
                entity.getId() != null ? entity.getId().toString() : null,
                entity.getEntity(),
                entity.getEntityId(),
                entity.getAction(),
                entity.getUserId() != null ? entity.getUserId().toString() : null,
                entity.getEndpoint(),
                entity.getIpAddress(),
                entity.getUserAgent(),
                deserializeJson(entity.getOldValue()),
                deserializeJson(entity.getNewValue()),
                entity.getCreatedAt()
        );
    }

    static Map<String, Object> deserializeJson(String json) {
        return JsonUtils.deserialize(json);
    }

    static String serializeJson(Map<String, Object> map) {
        return JsonUtils.serialize(map);
    }
}
