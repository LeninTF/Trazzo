package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import org.junit.jupiter.api.Test;
import trazzo.back.corehr.application.dto.result.ScheduleResult;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduleResponseTest {

    @Test
    void fromMapsAllFieldsWithNestedObjects() {
        var now = LocalDateTime.now();
        var entry = LocalTime.of(8, 0);
        var departure = LocalTime.of(17, 0);
        var toleranciaResult = new trazzo.back.corehr.application.dto.result.ToleranciaResult(
                1L, 10L, "T1", null, 15, "desc", true, now, now);
        var shiftResult = new ScheduleResult.ShiftSummary(99L, "Morning");
        var result = new ScheduleResult(1L, 99L, shiftResult, "Schedule1", "desc",
                entry, departure, List.of(toleranciaResult), now, now);

        var response = ScheduleResponse.from(result);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.shiftId()).isEqualTo(99L);
        assertThat(response.shift()).isNotNull();
        assertThat(response.shift().id()).isEqualTo(99L);
        assertThat(response.shift().name()).isEqualTo("Morning");
        assertThat(response.name()).isEqualTo("Schedule1");
        assertThat(response.tolerancias()).hasSize(1);
        assertThat(response.tolerancias().get(0).id()).isEqualTo(1L);
    }

    @Test
    void fromHandlesNullShift() {
        var now = LocalDateTime.now();
        var result = new ScheduleResult(1L, null, null, "Solo", null,
                LocalTime.of(8, 0), LocalTime.of(17, 0), List.of(), now, now);

        var response = ScheduleResponse.from(result);

        assertThat(response.shift()).isNull();
        assertThat(response.tolerancias()).isEmpty();
    }

    @Test
    void fromHandlesNullTolerancias() {
        var now = LocalDateTime.now();
        var result = new ScheduleResult(1L, null, null, "Solo", null,
                LocalTime.of(8, 0), LocalTime.of(17, 0), null, now, now);

        var response = ScheduleResponse.from(result);

        assertThat(response.tolerancias()).isEmpty();
    }

    @Test
    void innerRecord() {
        var summary = new ScheduleResponse.ShiftSummaryResponse(1L, "Night");
        assertThat(summary.id()).isEqualTo(1L);
        assertThat(summary.name()).isEqualTo("Night");
    }

    @Test
    void equalsAndHashCode() {
        var now = LocalDateTime.now();
        var a = new ScheduleResponse(1L, null, null, "n", null,
                LocalTime.of(8, 0), LocalTime.of(17, 0), List.of(), now, now);
        var b = new ScheduleResponse(1L, null, null, "n", null,
                LocalTime.of(8, 0), LocalTime.of(17, 0), List.of(), now, now);

        assertThat(a).isEqualTo(b);
        assertThat(a).hasSameHashCodeAs(b);
    }
}
