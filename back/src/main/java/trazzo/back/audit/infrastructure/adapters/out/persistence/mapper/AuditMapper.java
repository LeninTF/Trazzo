package trazzo.back.audit.infrastructure.adapters.out.persistence.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import trazzo.back.audit.domain.model.master.Audit;
import trazzo.back.audit.infrastructure.adapters.out.persistence.entity.AuditEntity;

import java.util.Map;
import java.util.UUID;

public final class AuditMapper {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AuditMapper() {
    }

    public static AuditEntity toEntity(Audit domain) {
        var entity = new AuditEntity();
        entity.setId(UUID.randomUUID());
        entity.setEntity(domain.getEntity());
        entity.setEntityId(domain.getEntityId());
        entity.setAction(domain.getAction());
        entity.setUserId(domain.getUserId() != null ? UUID.fromString(domain.getUserId()) : null);
        entity.setEndpoint(domain.getEndpoint());
        entity.setIpAdress(domain.getIpAdress());
        entity.setUserAgent(domain.getUserAgent());
        entity.setOldValue(serializeJson(domain.getPreviousValue()));
        entity.setNewValue(serializeJson(domain.getNewValue()));
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }

    public static Audit toDomain(AuditEntity entity) {
        return Audit.restore(
                0L,
                entity.getEntity(),
                entity.getEntityId(),
                entity.getAction(),
                entity.getUserId() != null ? entity.getUserId().toString() : null,
                entity.getEndpoint(),
                entity.getIpAdress(),
                entity.getUserAgent(),
                deserializeJson(entity.getOldValue()),
                deserializeJson(entity.getNewValue()),
                entity.getCreatedAt()
        );
    }

    static Map<String, Object> deserializeJson(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    static String serializeJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(map);
        } catch (Exception e) {
            return null;
        }
    }
}
