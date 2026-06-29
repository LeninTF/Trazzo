package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;

class PatchNonWorkingDayRequestTest {
    @Test
    void constructorAndGetters() {
        var d = LocalDate.of(2025, 12, 25);
        var r = new PatchNonWorkingDayRequest(d, "Christmas", true);
        assertThat(r.date()).isEqualTo(d);
        assertThat(r.description()).isEqualTo("Christmas");
        assertThat(r.isRecurring()).isTrue();
    }
    @Test
    void allowsAllNull() {
        var r = new PatchNonWorkingDayRequest(null, null, null);
        assertThat(r.date()).isNull();
        assertThat(r.isRecurring()).isNull();
    }
    @Test
    void equalsAndHashCode() {
        assertThat(new PatchNonWorkingDayRequest(null, null, null))
                .isEqualTo(new PatchNonWorkingDayRequest(null, null, null));
    }
}
