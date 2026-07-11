package trazzo.back.audit.infrastructure.adapters.out.persistence.mapper;

import org.junit.jupiter.api.Test;
import trazzo.back.audit.domain.model.tenant.HttpMethod;
import trazzo.back.audit.domain.model.tenant.SystemAudit;
import trazzo.back.audit.infrastructure.adapters.out.persistence.entity.SystemAuditEntity;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SystemAuditMapperTest {

    @Test
    void shouldMapToEntity() {
        var now = LocalDateTime.now();
        var domain = new SystemAudit(1L, "tenant-user-1", null, "Users",
                "User", "user-1", HttpMethod.POST, "/api/users",
                "Created user", Map.of("role", "admin"), Map.of("role", "user"),
                "192.168.1.1", "success", now);

        var entity = SystemAuditMapper.toEntity(domain);

        assertEquals(1L, entity.getId());
        assertEquals("POST", entity.getAccionSistema());
        assertEquals("Users", entity.getModulo());
        assertEquals("user-1", entity.getEntidadId());
        assertEquals("/api/users", entity.getEndpoint());
        assertEquals("Created user", entity.getDescripcion());
        assertEquals("192.168.1.1", entity.getIpAddress());
        assertEquals("success", entity.getResultado());
        assertEquals(now, entity.getDate());
        assertNotNull(entity.getValorAnterior());
        assertNotNull(entity.getValorNuevo());
    }

    @Test
    void shouldMapToDomain() {
        var now = LocalDateTime.now();
        var entity = new SystemAuditEntity();
        entity.setId(1L);
        entity.setAccionSistema("GET");
        entity.setModulo("Roles");
        entity.setEntidadId("role-1");
        entity.setEndpoint("/api/roles");
        entity.setDescripcion("Fetched role");
        entity.setValorAnterior("{\"old\":\"value\"}");
        entity.setValorNuevo("{\"new\":\"value\"}");
        entity.setIpAddress("10.0.0.1");
        entity.setResultado("ok");
        entity.setDate(now);

        var domain = SystemAuditMapper.toDomain(entity);

        assertEquals(1L, domain.getId());
        assertNull(domain.getUserTenantId());
        assertNull(domain.getSystemActions());
        assertEquals("Roles", domain.getModule());
        assertNull(domain.getEntity());
        assertEquals("role-1", domain.getEntityId());
        assertEquals(HttpMethod.GET, domain.getHttpMethod());
        assertEquals("/api/roles", domain.getEndpoint());
        assertEquals("Fetched role", domain.getDescription());
        assertEquals(Map.of("old", "value"), domain.getPreviousValue());
        assertEquals(Map.of("new", "value"), domain.getNewValue());
        assertEquals("10.0.0.1", domain.getIpAddress());
        assertEquals("ok", domain.getResult());
        assertEquals(now, domain.getCreatedAt());
    }

    @Test
    void shouldMapRoundTrip() {
        var now = LocalDateTime.now();
        var original = new SystemAudit(2L, "tenant-user-2", null, "Permissions",
                "Permission", "perm-1", HttpMethod.DELETE, "/api/permissions/perm-1",
                "Deleted permission", null, new java.util.HashMap<>(Map.of("deleted", true)),
                "192.168.1.2", "deleted", now);

        var entity = SystemAuditMapper.toEntity(original);
        var restored = SystemAuditMapper.toDomain(entity);

        assertEquals(original.getId(), restored.getId());
        assertEquals(original.getModule(), restored.getModule());
        assertEquals(original.getEntityId(), restored.getEntityId());
        assertEquals(original.getHttpMethod(), restored.getHttpMethod());
        assertEquals(original.getEndpoint(), restored.getEndpoint());
        assertEquals(original.getDescription(), restored.getDescription());
        assertEquals(original.getIpAddress(), restored.getIpAddress());
        assertEquals(original.getResult(), restored.getResult());
        assertEquals(original.getCreatedAt(), restored.getCreatedAt());
    }

    @Test
    void shouldHandleNullHttpMethodInEntity() {
        var now = LocalDateTime.now();
        var entity = new SystemAuditEntity();
        entity.setId(3L);
        entity.setAccionSistema("INVALID_METHOD");
        entity.setModulo("Test");
        entity.setEntidadId("test-1");
        entity.setEndpoint("/test");
        entity.setDescripcion("Test");
        entity.setIpAddress("0.0.0.0");
        entity.setResultado("ok");
        entity.setDate(now);

        var domain = SystemAuditMapper.toDomain(entity);

        assertNull(domain.getHttpMethod());
    }

    @Test
    void shouldHandleNullMaps() {
        var now = LocalDateTime.now();
        var entity = new SystemAuditEntity();
        entity.setId(4L);
        entity.setAccionSistema("PUT");
        entity.setModulo("Test");
        entity.setEntidadId("test-2");
        entity.setEndpoint("/test");
        entity.setDescripcion("Test with null maps");
        entity.setValorAnterior(null);
        entity.setValorNuevo(null);
        entity.setIpAddress("0.0.0.0");
        entity.setResultado("ok");
        entity.setDate(now);

        var domain = SystemAuditMapper.toDomain(entity);

        assertEquals(Map.of(), domain.getPreviousValue());
        assertEquals(Map.of(), domain.getNewValue());
    }
}
