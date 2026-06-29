package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import org.junit.jupiter.api.Test;
import trazzo.back.corehr.application.dto.result.AttendanceResult;
import trazzo.back.corehr.domain.model.AttendanceState;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AttendanceResponseTest {

    @Test
    void fromMapsAllFieldsWithNestedObjects() {
        var now = LocalDateTime.now();
        var user = new AttendanceResult.TenantUserBasicInfo("u1", "Juan", "Perez");
        var schedule = new trazzo.back.corehr.application.dto.result.ShiftResult.ScheduleSummary(99L, "Morning");
        var result = new AttendanceResult("id-1", 10L, user, 99L, schedule, 5L, "DVC-001",
                now, now.plusHours(8), LocalDate.now(), 0, AttendanceState.PUNTUAL, now, now);

        var response = AttendanceResponse.from(result);

        assertThat(response.id()).isEqualTo("id-1");
        assertThat(response.tenantUserId()).isEqualTo(10L);
        assertThat(response.tenantUser()).isNotNull();
        assertThat(response.tenantUser().id()).isEqualTo("u1");
        assertThat(response.tenantUser().nombre()).isEqualTo("Juan");
        assertThat(response.tenantUser().apellidoPaterno()).isEqualTo("Perez");
        assertThat(response.scheduleId()).isEqualTo(99L);
        assertThat(response.schedule()).isNotNull();
        assertThat(response.schedule().id()).isEqualTo(99L);
        assertThat(response.schedule().name()).isEqualTo("Morning");
        assertThat(response.deviceId()).isEqualTo(5L);
        assertThat(response.deviceCode()).isEqualTo("DVC-001");
        assertThat(response.checkIn()).isEqualTo(now);
        assertThat(response.state()).isEqualTo(AttendanceState.PUNTUAL);
    }

    @Test
    void fromHandlesNullNestedObjects() {
        var now = LocalDateTime.now();
        var result = new AttendanceResult("id-2", 20L, null, null, null, null, null,
                now, null, LocalDate.now(), 0, AttendanceState.TARDANZA, now, now);

        var response = AttendanceResponse.from(result);

        assertThat(response.id()).isEqualTo("id-2");
        assertThat(response.tenantUser()).isNull();
        assertThat(response.schedule()).isNull();
        assertThat(response.scheduleId()).isNull();
        assertThat(response.deviceId()).isNull();
    }

    @Test
    void innerRecords() {
        var user = new AttendanceResponse.TenantUserBasicInfoResponse("u1", "Ana", "Lopez");
        assertThat(user.id()).isEqualTo("u1");
        assertThat(user.nombre()).isEqualTo("Ana");
        assertThat(user.apellidoPaterno()).isEqualTo("Lopez");

        var schedule = new AttendanceResponse.ScheduleSummaryResponse(1L, "Night");
        assertThat(schedule.id()).isEqualTo(1L);
        assertThat(schedule.name()).isEqualTo("Night");
    }

    @Test
    void equalsAndHashCode() {
        var now = LocalDateTime.now();
        var a = new AttendanceResponse("id", 1L, null, null, null, null, null,
                now, null, LocalDate.now(), 0, AttendanceState.PUNTUAL, now, now);
        var b = new AttendanceResponse("id", 1L, null, null, null, null, null,
                now, null, LocalDate.now(), 0, AttendanceState.PUNTUAL, now, now);

        assertThat(a).isEqualTo(b);
        assertThat(a).hasSameHashCodeAs(b);
    }
}
