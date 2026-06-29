package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class CreateScheduleRequestTest {

    @Test
    void constructorAndGetters() {
        var entry = LocalTime.of(8, 0);
        var departure = LocalTime.of(17, 0);
        var request = new CreateScheduleRequest(1L, "Morning Shift", "desc", entry, departure);

        assertThat(request.shiftId()).isEqualTo(1L);
        assertThat(request.name()).isEqualTo("Morning Shift");
        assertThat(request.description()).isEqualTo("desc");
        assertThat(request.entryTime()).isEqualTo(entry);
        assertThat(request.departureTime()).isEqualTo(departure);
    }

    @Test
    void allowsNullDescription() {
        var request = new CreateScheduleRequest(1L, "name", null,
                LocalTime.of(8, 0), LocalTime.of(17, 0));

        assertThat(request.description()).isNull();
    }

    @Test
    void equalsAndHashCode() {
        var a = new CreateScheduleRequest(1L, "n", "d", LocalTime.of(8, 0), LocalTime.of(17, 0));
        var b = new CreateScheduleRequest(1L, "n", "d", LocalTime.of(8, 0), LocalTime.of(17, 0));

        assertThat(a).isEqualTo(b);
        assertThat(a).hasSameHashCodeAs(b);
    }
}
