package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import org.junit.jupiter.api.Test;
import java.time.LocalTime;
import static org.assertj.core.api.Assertions.assertThat;

class PatchScheduleRequestTest {
    @Test
    void constructorAndGetters() {
        var entry = LocalTime.of(8, 0);
        var dep = LocalTime.of(17, 0);
        var r = new PatchScheduleRequest("Morning", "desc", entry, dep);
        assertThat(r.name()).isEqualTo("Morning");
        assertThat(r.description()).isEqualTo("desc");
        assertThat(r.entryTime()).isEqualTo(entry);
        assertThat(r.departureTime()).isEqualTo(dep);
    }
    @Test
    void allowsAllNull() {
        var r = new PatchScheduleRequest(null, null, null, null);
        assertThat(r.name()).isNull();
        assertThat(r.entryTime()).isNull();
    }
    @Test
    void equalsAndHashCode() {
        assertThat(new PatchScheduleRequest(null, null, null, null))
                .isEqualTo(new PatchScheduleRequest(null, null, null, null));
    }
}
