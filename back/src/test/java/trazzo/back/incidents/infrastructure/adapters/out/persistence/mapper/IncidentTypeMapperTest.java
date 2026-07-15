package trazzo.back.incidents.infrastructure.adapters.out.persistence.mapper;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import trazzo.back.incidents.domain.model.IncidentType;
import trazzo.back.incidents.infrastructure.adapters.out.persistence.entity.IncidentTypeEntity;

import java.time.LocalDateTime;

class IncidentTypeMapperTest {

    @Test
    void toEntityMapsAllFields() {
        var now = LocalDateTime.now();
        var domain = IncidentType.restore("1", "Permiso", "Desc", true, now, now);
        var entity = IncidentTypeMapper.toEntity(domain);

        assertEquals(1, entity.getId());
        assertEquals("Permiso", entity.getNombre());
        assertEquals("Desc", entity.getDescripcion());
        assertTrue(entity.isActivo());
        assertEquals(now, entity.getCreatedAt());
        assertEquals(now, entity.getUpdatedAt());
    }

    @Test
    void toDomainMapsAllFields() {
        var now = LocalDateTime.now();
        var entity = new IncidentTypeEntity(1, "Permiso", "Desc", true, now, now);
        var domain = IncidentTypeMapper.toDomain(entity);

        assertEquals("1", domain.getId());
        assertEquals("Permiso", domain.getNombre());
        assertEquals("Desc", domain.getDescripcion());
        assertTrue(domain.isActivo());
        assertEquals(now, domain.getCreatedAt());
        assertEquals(now, domain.getUpdatedAt());
    }

    @Test
    void roundTripPreservesData() {
        var now = LocalDateTime.now();
        var original = IncidentType.restore("1", "Permiso", "Desc", false, now, now);
        var entity = IncidentTypeMapper.toEntity(original);
        var restored = IncidentTypeMapper.toDomain(entity);

        assertEquals(original.getId(), restored.getId());
        assertEquals(original.getNombre(), restored.getNombre());
        assertEquals(original.getDescripcion(), restored.getDescripcion());
        assertEquals(original.isActivo(), restored.isActivo());
    }

    @Test
    void nullIdMapsToNull() {
        var entity = new IncidentTypeEntity();
        entity.setNombre("Test");
        var domain = IncidentTypeMapper.toDomain(entity);
        assertNull(domain.getId());
    }

    @Test
    void nonNumericStringIdMapsToNull() {
        var domain = IncidentType.restore("abc", "Test", null, true, LocalDateTime.now(), LocalDateTime.now());
        var entity = IncidentTypeMapper.toEntity(domain);
        assertNull(entity.getId());
    }
}
