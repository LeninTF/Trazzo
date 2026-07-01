package trazzo.back.corehr.infrastructure.adapters.out.persistence.mapper;

import org.junit.jupiter.api.Test;
import trazzo.back.corehr.domain.model.AttendanceState;
import trazzo.back.corehr.domain.model.attendance.Attendance;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.entity.AttendanceEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AttendanceMapperTest {

    @Test
    void shouldMapToEntity() {
        var now = LocalDateTime.now();
        var domain = Attendance.restore("id-1", 1L, 2L, 3L, now, now.plusHours(8),
                LocalDate.now(), 5, AttendanceState.PUNTUAL, now, now);

        var entity = AttendanceMapper.toEntity(domain);

        assertEquals("id-1", entity.getId());
        assertEquals(1L, entity.getTenantUserId());
        assertEquals(2L, entity.getScheduleId());
        assertEquals(3L, entity.getDeviceId());
        assertEquals(now, entity.getCheckIn());
        assertEquals(now.plusHours(8), entity.getCheckOut());
        assertEquals(LocalDate.now(), entity.getAttendanceDate());
        assertEquals(5, entity.getMinutesLate());
        assertEquals(AttendanceState.PUNTUAL, entity.getState());
        assertEquals(now, entity.getCreatedAt());
        assertEquals(now, entity.getUpdatedAt());
    }

    @Test
    void shouldMapToDomain() {
        var now = LocalDateTime.now();
        var entity = new AttendanceEntity();
        entity.setId("id-1");
        entity.setTenantUserId(1L);
        entity.setScheduleId(2L);
        entity.setDeviceId(3L);
        entity.setCheckIn(now);
        entity.setCheckOut(now.plusHours(8));
        entity.setAttendanceDate(LocalDate.now());
        entity.setMinutesLate(5);
        entity.setState(AttendanceState.PUNTUAL);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        var domain = AttendanceMapper.toDomain(entity);

        assertEquals("id-1", domain.getId());
        assertEquals(1L, domain.getTenantUserId());
        assertEquals(2L, domain.getScheduleId());
        assertEquals(3L, domain.getDeviceId());
        assertEquals(now, domain.getCheckIn());
        assertEquals(now.plusHours(8), domain.getCheckOut());
        assertEquals(5, domain.getMinutesLate());
        assertEquals(AttendanceState.PUNTUAL, domain.getState());
    }

    @Test
    void shouldHandleNullFieldsInEntity() {
        var now = LocalDateTime.now();
        var entity = new AttendanceEntity();
        entity.setId("id-2");
        entity.setTenantUserId(1L);
        entity.setAttendanceDate(LocalDate.now());
        entity.setMinutesLate(0);
        entity.setState(AttendanceState.TARDANZA);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        var domain = AttendanceMapper.toDomain(entity);

        assertEquals("id-2", domain.getId());
        assertNull(domain.getScheduleId());
        assertNull(domain.getDeviceId());
        assertNull(domain.getCheckIn());
        assertNull(domain.getCheckOut());
        assertEquals(AttendanceState.TARDANZA, domain.getState());
    }

    @Test
    void shouldMapRoundTrip() {
        var now = LocalDateTime.now();
        var original = Attendance.restore("id-3", 1L, 2L, 3L, now, now.plusHours(8),
                LocalDate.now(), 10, AttendanceState.TARDANZA, now, now);

        var entity = AttendanceMapper.toEntity(original);
        var restored = AttendanceMapper.toDomain(entity);

        assertEquals(original.getId(), restored.getId());
        assertEquals(original.getTenantUserId(), restored.getTenantUserId());
        assertEquals(original.getScheduleId(), restored.getScheduleId());
        assertEquals(original.getMinutesLate(), restored.getMinutesLate());
        assertEquals(original.getState(), restored.getState());
    }
}
