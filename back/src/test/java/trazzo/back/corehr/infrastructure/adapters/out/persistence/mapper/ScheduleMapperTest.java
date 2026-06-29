package trazzo.back.corehr.infrastructure.adapters.out.persistence.mapper;

import org.junit.jupiter.api.Test;
import trazzo.back.corehr.domain.model.schedule.Schedule;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.entity.ScheduleEntity;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class ScheduleMapperTest {

    @Test
    void shouldMapToEntity() {
        var now = LocalDateTime.now();
        var domain = Schedule.restore(1L, 10L, "Morning", "desc",
                LocalTime.of(8, 0), LocalTime.of(17, 0), now, now);

        var entity = ScheduleMapper.toEntity(domain);

        assertEquals(1L, entity.getId());
        assertEquals(10L, entity.getShiftId());
        assertEquals("Morning", entity.getName());
        assertEquals("desc", entity.getDescription());
        assertEquals(LocalTime.of(8, 0), entity.getEntryTime());
        assertEquals(LocalTime.of(17, 0), entity.getDepartureTime());
    }

    @Test
    void shouldMapToDomain() {
        var now = LocalDateTime.now();
        var entity = new ScheduleEntity();
        entity.setId(1L);
        entity.setShiftId(10L);
        entity.setName("Morning");
        entity.setDescription("desc");
        entity.setEntryTime(LocalTime.of(8, 0));
        entity.setDepartureTime(LocalTime.of(17, 0));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        var domain = ScheduleMapper.toDomain(entity);

        assertEquals(1L, domain.getId());
        assertEquals(10L, domain.getShiftId());
        assertEquals("Morning", domain.getName());
    }

    @Test
    void shouldMapRoundTrip() {
        var now = LocalDateTime.now();
        var original = Schedule.restore(2L, 20L, "Afternoon", null,
                LocalTime.of(13, 0), LocalTime.of(22, 0), now, now);

        var entity = ScheduleMapper.toEntity(original);
        var restored = ScheduleMapper.toDomain(entity);

        assertEquals(original.getId(), restored.getId());
        assertEquals(original.getName(), restored.getName());
        assertNull(restored.getDescription());
    }
}
