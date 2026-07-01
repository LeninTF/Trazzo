package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import org.junit.jupiter.api.Test;
import trazzo.back.corehr.application.dto.result.UserScheduleResult;
import trazzo.back.corehr.application.dto.result.ShiftResult;
import java.time.LocalDateTime;
import java.time.LocalTime;
import static org.assertj.core.api.Assertions.assertThat;

class UserScheduleResponseTest {

    @Test
    void fromMapsAllFieldsWithNestedSchedule() {
        var now = LocalDateTime.now();
        var entry = LocalTime.of(8, 0);
        var dep = LocalTime.of(17, 0);
        var scheduleSummary = new ShiftResult.ScheduleSummary(5L, "Morning");
        var result = new UserScheduleResult(1L, 10L, 5L, scheduleSummary, "desc", entry, dep, now, now);
        var response = UserScheduleResponse.from(result);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.tenantUserId()).isEqualTo(10L);
        assertThat(response.scheduleId()).isEqualTo(5L);
        assertThat(response.schedule()).isNotNull();
        assertThat(response.schedule().id()).isEqualTo(5L);
        assertThat(response.schedule().name()).isEqualTo("Morning");
        assertThat(response.description()).isEqualTo("desc");
        assertThat(response.entryTime()).isEqualTo(entry);
    }

    @Test
    void fromHandlesNullSchedule() {
        var now = LocalDateTime.now();
        var result = new UserScheduleResult(1L, 10L, null, null, null,
                LocalTime.of(8, 0), LocalTime.of(17, 0), now, now);
        var response = UserScheduleResponse.from(result);
        assertThat(response.schedule()).isNull();
        assertThat(response.scheduleId()).isNull();
    }

    @Test
    void innerRecord() {
        var s = new UserScheduleResponse.ScheduleSummaryResponse(1L, "Night");
        assertThat(s.id()).isEqualTo(1L);
        assertThat(s.name()).isEqualTo("Night");
    }

    @Test
    void equalsAndHashCode() {
        var now = LocalDateTime.now();
        var t = LocalTime.of(8, 0);
        var a = new UserScheduleResponse(1L, 10L, null, null, null, t, t, now, now);
        var b = new UserScheduleResponse(1L, 10L, null, null, null, t, t, now, now);
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }
}
