package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import org.junit.jupiter.api.Test;
import trazzo.back.corehr.application.dto.result.ShiftResult;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ShiftResponseTest {

    @Test
    void fromMapsAllFieldsWithSchedules() {
        var now = LocalDateTime.now();
        var scheduleSummary = new ShiftResult.ScheduleSummary(1L, "Morning");
        var result = new ShiftResult(10L, "Shift1", "desc", List.of(scheduleSummary), now, now);

        var response = ShiftResponse.from(result);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.name()).isEqualTo("Shift1");
        assertThat(response.description()).isEqualTo("desc");
        assertThat(response.schedules()).hasSize(1);
        assertThat(response.schedules().get(0).id()).isEqualTo(1L);
        assertThat(response.schedules().get(0).name()).isEqualTo("Morning");
        assertThat(response.createdAt()).isEqualTo(now);
        assertThat(response.updatedAt()).isEqualTo(now);
    }

    @Test
    void fromWithEmptyScheduleList() {
        var now = LocalDateTime.now();
        var result = new ShiftResult(10L, "Empty", "no schedules", List.of(), now, now);

        var response = ShiftResponse.from(result);

        assertThat(response.schedules()).isEmpty();
    }

    @Test
    void innerRecord() {
        var summary = new ShiftResponse.ScheduleSummaryResponse(5L, "Night");
        assertThat(summary.id()).isEqualTo(5L);
        assertThat(summary.name()).isEqualTo("Night");
    }

    @Test
    void equalsAndHashCode() {
        var now = LocalDateTime.now();
        var a = new ShiftResponse(1L, "n", "d", List.of(), now, now);
        var b = new ShiftResponse(1L, "n", "d", List.of(), now, now);

        assertThat(a).isEqualTo(b);
        assertThat(a).hasSameHashCodeAs(b);
    }
}
