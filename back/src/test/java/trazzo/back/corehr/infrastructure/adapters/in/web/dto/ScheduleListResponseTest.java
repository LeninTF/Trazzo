package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import org.junit.jupiter.api.Test;
import trazzo.back.corehr.application.dto.result.PaginatedResult;
import trazzo.back.corehr.application.dto.result.ScheduleResult;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class ScheduleListResponseTest {
    @Test
    void fromMapsPaginatedResult() {
        var now = LocalDateTime.now();
        var result = new ScheduleResult(1L, 10L, null, "Schedule1", null,
                LocalTime.of(8, 0), LocalTime.of(17, 0), List.of(), now, now);
        var paginated = new PaginatedResult<>(List.of(result), 1, 5, 20, 4);
        var response = ScheduleListResponse.from(paginated);
        assertThat(response.content()).hasSize(1);
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.totalPages()).isEqualTo(4);
    }
    @Test
    void fromEmptyContent() {
        var paginated = new PaginatedResult<ScheduleResult>(List.of(), 0, 10, 0, 0);
        assertThat(ScheduleListResponse.from(paginated).content()).isEmpty();
    }
    @Test
    void equalsAndHashCode() {
        assertThat(new ScheduleListResponse(List.of(), 0, 0, 0, 0))
                .isEqualTo(new ScheduleListResponse(List.of(), 0, 0, 0, 0));
    }
}
