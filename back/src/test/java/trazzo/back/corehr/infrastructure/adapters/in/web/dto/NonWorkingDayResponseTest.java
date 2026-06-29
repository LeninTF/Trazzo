package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import org.junit.jupiter.api.Test;
import trazzo.back.corehr.application.dto.result.NonWorkingDayResult;
import java.time.LocalDate;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;

class NonWorkingDayResponseTest {

    @Test
    void fromMapsAllFields() {
        var now = LocalDateTime.now();
        var date = LocalDate.of(2025, 12, 25);
        var result = new NonWorkingDayResult(1L, date, "Christmas", true, now);
        var response = NonWorkingDayResponse.from(result);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.date()).isEqualTo(date);
        assertThat(response.description()).isEqualTo("Christmas");
        assertThat(response.isRecurring()).isTrue();
        assertThat(response.createdAt()).isEqualTo(now);
    }

    @Test
    void fromHandlesNullDescription() {
        var now = LocalDateTime.now();
        var result = new NonWorkingDayResult(1L, LocalDate.now(), null, false, now);
        var response = NonWorkingDayResponse.from(result);
        assertThat(response.description()).isNull();
    }

    @Test
    void equalsAndHashCode() {
        var now = LocalDateTime.now();
        var d = LocalDate.now();
        var a = new NonWorkingDayResponse(1L, d, null, false, now);
        var b = new NonWorkingDayResponse(1L, d, null, false, now);
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }
}
