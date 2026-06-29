package trazzo.back.corehr.infrastructure.adapters.out.persistence.mapper;

import org.junit.jupiter.api.Test;
import trazzo.back.corehr.domain.model.schedule.UserSchedule;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.entity.UserScheduleEntity;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class UserScheduleMapperTest {

    @Test
    void shouldMapToEntity() {
        var now = LocalDateTime.now();
        var domain = UserSchedule.restore(1L, 10L, 20L, "desc",
                LocalTime.of(8, 0), LocalTime.of(17, 0), now, now);

        var entity = UserScheduleMapper.toEntity(domain);

        assertEquals(1L, entity.getId());
        assertEquals(10L, entity.getTenantUserId());
        assertEquals(20L, entity.getScheduleId());
        assertEquals("desc", entity.getDescription());
        assertEquals(LocalTime.of(8, 0), entity.getEntryTime());
        assertEquals(LocalTime.of(17, 0), entity.getDepartureTime());
    }

    @Test
    void shouldMapToDomain() {
        var now = LocalDateTime.now();
        var entity = new UserScheduleEntity();
        entity.setId(1L);
        entity.setTenantUserId(10L);
        entity.setScheduleId(20L);
        entity.setDescription("desc");
        entity.setEntryTime(LocalTime.of(8, 0));
        entity.setDepartureTime(LocalTime.of(17, 0));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        var domain = UserScheduleMapper.toDomain(entity);

        assertEquals(1L, domain.getId());
        assertEquals(10L, domain.getTenantUserId());
        assertEquals(20L, domain.getScheduleId());
    }

    @Test
    void shouldHandleNullDescription() {
        var now = LocalDateTime.now();
        var domain = UserSchedule.restore(2L, 20L, 30L, null,
                LocalTime.of(9, 0), LocalTime.of(18, 0), now, now);

        var entity = UserScheduleMapper.toEntity(domain);
        var restored = UserScheduleMapper.toDomain(entity);

        assertNull(restored.getDescription());
    }
}
