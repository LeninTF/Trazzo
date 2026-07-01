package trazzo.back.audit.infrastructure.adapters.out.persistence.mapper;

import org.junit.jupiter.api.Test;
import trazzo.back.audit.domain.model.master.Action;
import trazzo.back.audit.domain.model.master.Audit;
import trazzo.back.audit.infrastructure.adapters.out.persistence.entity.AuditEntity;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AuditMapperTest {

    @Test
    void shouldMapToEntity() {
        var now = LocalDateTime.now();
        var domain = Audit.restore("00000000-0000-0000-0000-000000000001", "User", "user-1", Action.CREATE, "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "/api/users", "192.168.1.1", "Mozilla/5.0",
                Map.of("old", "value1"), Map.of("new", "value2"), now);

        var entity = AuditMapper.toEntity(domain);

        assertEquals("User", entity.getEntity());
        assertEquals("user-1", entity.getEntityId());
        assertEquals(Action.CREATE, entity.getAction());
        assertEquals("a1b2c3d4-e5f6-7890-abcd-ef1234567890", entity.getUserId().toString());
        assertEquals("/api/users", entity.getEndpoint());
        assertEquals("192.168.1.1", entity.getIpAdress());
        assertEquals("Mozilla/5.0", entity.getUserAgent());
        assertEquals(now, entity.getCreatedAt());
        assertNotNull(entity.getOldValue());
        assertNotNull(entity.getNewValue());
    }

    @Test
    void shouldMapToDomain() {
        var now = LocalDateTime.now();
        var entity = new AuditEntity();
        entity.setId(UUID.randomUUID());
        entity.setEntity("Role");
        entity.setEntityId("role-1");
        entity.setAction(Action.UPDATE);
        entity.setUserId(UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f12345678901"));
        entity.setEndpoint("/api/roles");
        entity.setIpAdress("10.0.0.1");
        entity.setUserAgent("curl/7.68");
        entity.setOldValue("{\"key\":\"old\"}");
        entity.setNewValue("{\"key\":\"new\"}");
        entity.setCreatedAt(now);

        var domain = AuditMapper.toDomain(entity);

        assertEquals("Role", domain.getEntity());
        assertEquals("role-1", domain.getEntityId());
        assertEquals(Action.UPDATE, domain.getAction());
        assertEquals("b2c3d4e5-f6a7-8901-bcde-f12345678901", domain.getUserId());
        assertEquals("/api/roles", domain.getEndpoint());
        assertEquals("10.0.0.1", domain.getIpAdress());
        assertEquals("curl/7.68", domain.getUserAgent());
        assertEquals(Map.of("key", "old"), domain.getPreviousValue());
        assertEquals(Map.of("key", "new"), domain.getNewValue());
        assertEquals(now, domain.getCreatedAt());
    }

    @Test
    void shouldMapRoundTrip() {
        var now = LocalDateTime.now();
        var original = Audit.restore("00000000-0000-0000-0000-000000000001", "Permission", "perm-1", Action.DELETE, null,
                "/api/permissions", "192.168.1.100", null,
                Map.of(), new java.util.HashMap<>(Map.of("enabled", false)), now);

        var entity = AuditMapper.toEntity(original);
        var restored = AuditMapper.toDomain(entity);

        assertEquals(original.getEntity(), restored.getEntity());
        assertEquals(original.getEntityId(), restored.getEntityId());
        assertEquals(original.getAction(), restored.getAction());
        assertEquals(original.getEndpoint(), restored.getEndpoint());
        assertEquals(original.getIpAdress(), restored.getIpAdress());
        assertEquals(original.getCreatedAt(), restored.getCreatedAt());
    }

    @Test
    void shouldHandleNullUserId() {
        var entity = new AuditEntity();
        entity.setId(UUID.randomUUID());
        entity.setEntity("Test");
        entity.setAction(Action.CREATE);
        entity.setCreatedAt(LocalDateTime.now());

        var domain = AuditMapper.toDomain(entity);

        assertNull(domain.getUserId());
    }

    @Test
    void shouldHandleNullJsonFields() {
        var entity = new AuditEntity();
        entity.setId(UUID.randomUUID());
        entity.setEntity("Test");
        entity.setAction(Action.CREATE);
        entity.setCreatedAt(LocalDateTime.now());

        var domain = AuditMapper.toDomain(entity);

        assertEquals(Map.of(), domain.getPreviousValue());
        assertEquals(Map.of(), domain.getNewValue());
    }

    @Test
    void shouldSerializeAndDeserializeJson() {
        Map<String, Object> map = new java.util.HashMap<>(Map.<String, Object>of("name", "test", "value", 42));
        var json = AuditMapper.serializeJson(map);
        assertNotNull(json);

        var result = AuditMapper.deserializeJson(json);
        assertEquals(map, result);
    }

    @Test
    void shouldReturnEmptyMapForNullJson() {
        assertEquals(Map.of(), AuditMapper.deserializeJson(null));
        assertEquals(Map.of(), AuditMapper.deserializeJson(""));
    }

    @Test
    void shouldReturnNullForEmptyOrNullMap() {
        assertNull(AuditMapper.serializeJson(Map.of()));
        assertNull(AuditMapper.serializeJson(null));
    }
}
