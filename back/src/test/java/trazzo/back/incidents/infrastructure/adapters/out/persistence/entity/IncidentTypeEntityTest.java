package trazzo.back.incidents.infrastructure.adapters.out.persistence.entity;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

class IncidentTypeEntityTest {

    @Test
    void createInstance() {
        var now = LocalDateTime.now();
        var entity = new IncidentTypeEntity("id-1", "Permiso", "Desc", true, now, now);

        assertEquals("id-1", entity.getId());
        assertEquals("Permiso", entity.getNombre());
        assertEquals("Desc", entity.getDescripcion());
        assertTrue(entity.isActivo());
        assertEquals(now, entity.getCreatedAt());
        assertEquals(now, entity.getUpdatedAt());
    }

    @Test
    void settersWorkCorrectly() {
        var entity = new IncidentTypeEntity();
        entity.setId("id-1");
        entity.setNombre("Permiso");
        entity.setDescripcion("Desc");
        entity.setActivo(false);

        assertEquals("id-1", entity.getId());
        assertEquals("Permiso", entity.getNombre());
        assertFalse(entity.isActivo());
    }
}
