package trazzo.back.corehr.infrastructure.adapters.out.persistence.mapper;

import org.junit.jupiter.api.Test;
import trazzo.back.corehr.domain.model.ToleranciaType;
import trazzo.back.corehr.domain.model.schedule.Tolerancia;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.entity.ToleranciaEntity;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ToleranciaMapperTest {

    @Test
    void shouldMapToEntity() {
        var now = LocalDateTime.now();
        var domain = Tolerancia.restore(1L, 10L, "T1", ToleranciaType.ENTRADA, 5, "desc", true, now, now);

        var entity = ToleranciaMapper.toEntity(domain);

        assertEquals(1L, entity.getId());
        assertEquals(10L, entity.getScheduleId());
        assertEquals("T1", entity.getName());
        assertEquals(ToleranciaType.ENTRADA, entity.getType());
        assertEquals(5, entity.getMinutes());
        assertEquals("desc", entity.getDescription());
        assertTrue(entity.isActivo());
    }

    @Test
    void shouldMapToDomain() {
        var now = LocalDateTime.now();
        var entity = new ToleranciaEntity();
        entity.setId(1L);
        entity.setScheduleId(10L);
        entity.setName("T1");
        entity.setType(ToleranciaType.SALIDA);
        entity.setMinutes(15);
        entity.setDescription("desc");
        entity.setActivo(true);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        var domain = ToleranciaMapper.toDomain(entity);

        assertEquals(1L, domain.getId());
        assertEquals(ToleranciaType.SALIDA, domain.getType());
        assertTrue(domain.isActivo());
    }

    @Test
    void shouldHandleInactive() {
        var now = LocalDateTime.now();
        var domain = Tolerancia.restore(2L, 20L, "T2", ToleranciaType.HORAS_EXTRA, 30, null, false, now, now);

        var entity = ToleranciaMapper.toEntity(domain);
        var restored = ToleranciaMapper.toDomain(entity);

        assertFalse(restored.isActivo());
        assertNull(restored.getDescription());
    }
}
