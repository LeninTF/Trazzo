package trazzo.back.corehr.infrastructure.adapters.out.persistence.mapper;

import org.junit.jupiter.api.Test;
import trazzo.back.corehr.domain.model.schedule.NonWorkingDays;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.entity.NonWorkingDaysEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class NonWorkingDaysMapperTest {

    @Test
    void shouldMapToEntity() {
        var now = LocalDateTime.now();
        var domain = NonWorkingDays.restore(1L, LocalDate.of(2026, 1, 1), "New Year", true, now);

        var entity = NonWorkingDaysMapper.toEntity(domain);

        assertEquals(1L, entity.getId());
        assertEquals(LocalDate.of(2026, 1, 1), entity.getDate());
        assertEquals("New Year", entity.getDescription());
        assertTrue(entity.isRecurring());
    }

    @Test
    void shouldMapToDomain() {
        var now = LocalDateTime.now();
        var entity = new NonWorkingDaysEntity();
        entity.setId(1L);
        entity.setDate(LocalDate.of(2026, 12, 25));
        entity.setDescription("Christmas");
        entity.setRecurring(false);
        entity.setCreatedAt(now);

        var domain = NonWorkingDaysMapper.toDomain(entity);

        assertEquals(1L, domain.getId());
        assertEquals(LocalDate.of(2026, 12, 25), domain.getDate());
        assertFalse(domain.isRecurring());
    }

    @Test
    void shouldHandleNullDescription() {
        var now = LocalDateTime.now();
        var domain = NonWorkingDays.restore(2L, LocalDate.of(2026, 5, 1), null, false, now);

        var entity = NonWorkingDaysMapper.toEntity(domain);
        var restored = NonWorkingDaysMapper.toDomain(entity);

        assertNull(restored.getDescription());
        assertFalse(restored.isRecurring());
    }
}
