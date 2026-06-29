package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import org.junit.jupiter.api.Test;
import trazzo.back.corehr.application.dto.result.PaginatedResult;
import trazzo.back.corehr.application.dto.result.UserScheduleResult;
import trazzo.back.corehr.application.dto.result.ShiftResult;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class UserScheduleListResponseTest {
    @Test
    void fromMapsPaginatedResult() {
        var now = LocalDateTime.now();
        var summary = new ShiftResult.ScheduleSummary(5L, "Morning");
        var result = new UserScheduleResult(1L, 10L, 5L, summary, "desc",
                LocalTime.of(8, 0), LocalTime.of(17, 0), now, now);
        var paginated = new PaginatedResult<>(List.of(result), 0, 10, 1, 1);
        var response = UserScheduleListResponse.from(paginated);
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).schedule()).isNotNull();
    }
    @Test
    void fromEmptyContent() {
        var paginated = new PaginatedResult<UserScheduleResult>(List.of(), 0, 10, 0, 0);
        assertThat(UserScheduleListResponse.from(paginated).content()).isEmpty();
    }
    @Test
    void equalsAndHashCode() {
        assertThat(new UserScheduleListResponse(List.of(), 0, 0, 0, 0))
                .isEqualTo(new UserScheduleListResponse(List.of(), 0, 0, 0, 0));
    }
}
