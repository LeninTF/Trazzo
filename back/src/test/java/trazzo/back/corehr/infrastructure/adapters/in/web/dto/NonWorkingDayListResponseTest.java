package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import org.junit.jupiter.api.Test;
import trazzo.back.corehr.application.dto.result.NonWorkingDayResult;
import trazzo.back.corehr.application.dto.result.PaginatedResult;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class NonWorkingDayListResponseTest {
    @Test
    void fromMapsPaginatedResult() {
        var now = LocalDateTime.now();
        var result = new NonWorkingDayResult(1L, LocalDate.of(2025, 12, 25), "Xmas", true, now);
        var paginated = new PaginatedResult<>(List.of(result), 0, 10, 1, 1);
        var response = NonWorkingDayListResponse.from(paginated);
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).description()).isEqualTo("Xmas");
    }
    @Test
    void fromEmptyContent() {
        var paginated = new PaginatedResult<NonWorkingDayResult>(List.of(), 0, 10, 0, 0);
        assertThat(NonWorkingDayListResponse.from(paginated).content()).isEmpty();
    }
    @Test
    void equalsAndHashCode() {
        assertThat(new NonWorkingDayListResponse(List.of(), 0, 0, 0, 0))
                .isEqualTo(new NonWorkingDayListResponse(List.of(), 0, 0, 0, 0));
    }
}
