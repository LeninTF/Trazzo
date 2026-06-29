package trazzo.back.corehr.application.dto.result;

import org.junit.jupiter.api.Test;
import trazzo.back.corehr.domain.model.AttendanceState;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AttendanceResultTest {

    @Test
    void shouldCreateAttendanceResult() {
        var now = LocalDateTime.now();
        var result = new AttendanceResult(
                "id-1", 1L, null, 2L, null, 3L, "DEV-001",
                now, now.plusHours(8), LocalDate.now(), 5, AttendanceState.PUNTUAL, now, now
        );
        assertEquals("id-1", result.id());
        assertEquals(1L, result.tenantUserId());
        assertEquals(2L, result.scheduleId());
        assertEquals(3L, result.deviceId());
        assertEquals("DEV-001", result.deviceCode());
        assertEquals(now, result.checkIn());
        assertEquals(now.plusHours(8), result.checkOut());
        assertEquals(5, result.minutesLate());
        assertEquals(AttendanceState.PUNTUAL, result.state());
    }

    @Test
    void shouldHandleNullOptionalFields() {
        var result = new AttendanceResult(
                "id-2", 1L, null, null, null, null, null,
                null, null, LocalDate.now(), 0, AttendanceState.TARDANZA, null, null
        );
        assertNull(result.tenantUser());
        assertNull(result.schedule());
        assertNull(result.scheduleId());
        assertNull(result.deviceId());
        assertNull(result.deviceCode());
        assertNull(result.checkIn());
        assertNull(result.checkOut());
        assertNull(result.createdAt());
        assertNull(result.updatedAt());
    }

    @Test
    void shouldCreateTenantUserBasicInfo() {
        var info = new AttendanceResult.TenantUserBasicInfo("u-1", "Juan", "Perez");
        assertEquals("u-1", info.id());
        assertEquals("Juan", info.nombre());
        assertEquals("Perez", info.apellidoPaterno());
    }

    @Test
    void shouldTestEquality() {
        var now = LocalDateTime.now();
        var r1 = new AttendanceResult("id-1", 1L, null, null, null, null, null,
                null, null, LocalDate.now(), 0, AttendanceState.PUNTUAL, now, now);
        var r2 = new AttendanceResult("id-1", 1L, null, null, null, null, null,
                null, null, LocalDate.now(), 0, AttendanceState.PUNTUAL, now, now);
        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void shouldTestToString() {
        var now = LocalDateTime.now();
        var result = new AttendanceResult("id-1", 1L, null, null, null, null, null,
                null, null, LocalDate.now(), 0, AttendanceState.PUNTUAL, now, now);
        assertTrue(result.toString().contains("id-1"));
    }
}
