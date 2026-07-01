package trazzo.back.corehr.infrastructure.adapters.out.persistence.mapper;

import org.junit.jupiter.api.Test;
import trazzo.back.corehr.domain.model.schedule.Shift;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.entity.ShiftEntity;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ShiftMapperTest {

    @Test
    void shouldMapToEntity() {
        var now = LocalDateTime.now();
        var domain = Shift.restore(1L, "Morning", "desc", now, now);

        var entity = ShiftMapper.toEntity(domain);

        assertEquals(1L, entity.getId());
        assertEquals("Morning", entity.getName());
        assertEquals("desc", entity.getDescription());
        assertEquals(now, entity.getCreatedAt());
        assertEquals(now, entity.getUpdatedAt());
    }

    @Test
    void shouldMapToDomain() {
        var now = LocalDateTime.now();
        var entity = new ShiftEntity();
        entity.setId(1L);
        entity.setName("Morning");
        entity.setDescription("desc");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        var domain = ShiftMapper.toDomain(entity);

        assertEquals(1L, domain.getId());
        assertEquals("Morning", domain.getName());
    }

    @Test
    void shouldHandleNullDescription() {
        var now = LocalDateTime.now();
        var domain = Shift.restore(2L, "Night", null, now, now);

        var entity = ShiftMapper.toEntity(domain);
        var restored = ShiftMapper.toDomain(entity);

        assertNull(entity.getDescription());
        assertNull(restored.getDescription());
    }
}
