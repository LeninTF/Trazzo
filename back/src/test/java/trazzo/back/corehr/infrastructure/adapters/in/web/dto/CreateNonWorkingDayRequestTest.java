package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;

class CreateNonWorkingDayRequestTest {
    @Test
    void constructorAndGetters() {
        var date = LocalDate.of(2025, 12, 25);
        var r = new CreateNonWorkingDayRequest(date, "Christmas", true);
        assertThat(r.date()).isEqualTo(date);
        assertThat(r.description()).isEqualTo("Christmas");
        assertThat(r.isRecurring()).isTrue();
    }
    @Test
    void allowsNullDescription() {
        var r = new CreateNonWorkingDayRequest(LocalDate.now(), null, false);
        assertThat(r.description()).isNull();
    }
    @Test
    void equalsAndHashCode() {
        var d = LocalDate.now();
        assertThat(new CreateNonWorkingDayRequest(d, "a", true))
                .isEqualTo(new CreateNonWorkingDayRequest(d, "a", true))
                .hasSameHashCodeAs(new CreateNonWorkingDayRequest(d, "a", true));
    }
}
