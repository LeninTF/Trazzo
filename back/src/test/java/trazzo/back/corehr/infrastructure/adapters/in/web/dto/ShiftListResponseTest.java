package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import org.junit.jupiter.api.Test;
import trazzo.back.corehr.application.dto.result.PaginatedResult;
import trazzo.back.corehr.application.dto.result.ShiftResult;
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class ShiftListResponseTest {
    @Test
    void fromMapsPaginatedResult() {
        var now = LocalDateTime.now();
        var result = new ShiftResult(1L, "Shift1", "desc", List.of(), now, now);
        var paginated = new PaginatedResult<>(List.of(result), 0, 20, 1, 1);
        var response = ShiftListResponse.from(paginated);
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).name()).isEqualTo("Shift1");
    }
    @Test
    void fromEmptyContent() {
        var paginated = new PaginatedResult<ShiftResult>(List.of(), 0, 10, 0, 0);
        assertThat(ShiftListResponse.from(paginated).content()).isEmpty();
    }
    @Test
    void equalsAndHashCode() {
        assertThat(new ShiftListResponse(List.of(), 0, 0, 0, 0))
                .isEqualTo(new ShiftListResponse(List.of(), 0, 0, 0, 0));
    }
}
