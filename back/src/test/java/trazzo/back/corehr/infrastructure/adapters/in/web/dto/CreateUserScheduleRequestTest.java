package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import org.junit.jupiter.api.Test;
import java.time.LocalTime;
import static org.assertj.core.api.Assertions.assertThat;

class CreateUserScheduleRequestTest {
    @Test
    void constructorAndGetters() {
        var entry = LocalTime.of(8, 0);
        var dep = LocalTime.of(17, 0);
        var r = new CreateUserScheduleRequest(10L, 5L, "desc", entry, dep);
        assertThat(r.tenantUserId()).isEqualTo(10L);
        assertThat(r.scheduleId()).isEqualTo(5L);
        assertThat(r.description()).isEqualTo("desc");
        assertThat(r.entryTime()).isEqualTo(entry);
        assertThat(r.departureTime()).isEqualTo(dep);
    }
    @Test
    void allowsNullDescription() {
        var r = new CreateUserScheduleRequest(1L, 1L, null, LocalTime.of(8, 0), LocalTime.of(17, 0));
        assertThat(r.description()).isNull();
    }
    @Test
    void equalsAndHashCode() {
        var t = LocalTime.of(8, 0);
        assertThat(new CreateUserScheduleRequest(1L, 1L, "d", t, t))
                .isEqualTo(new CreateUserScheduleRequest(1L, 1L, "d", t, t))
                .hasSameHashCodeAs(new CreateUserScheduleRequest(1L, 1L, "d", t, t));
    }
}
