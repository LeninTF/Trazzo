package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import org.junit.jupiter.api.Test;
import trazzo.back.corehr.application.dto.result.PaginatedResult;
import trazzo.back.corehr.application.dto.result.UserBiometriaResult;
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class UserBiometriaListResponseTest {
    @Test
    void fromMapsPaginatedResult() {
        var now = LocalDateTime.now();
        var result = new UserBiometriaResult(1L, 10L, 5L, "DVC-001", 3, true, now, now, now);
        var paginated = new PaginatedResult<>(List.of(result), 0, 10, 1, 1);
        var response = UserBiometriaListResponse.from(paginated);
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).fingerIndex()).isEqualTo(3);
    }
    @Test
    void fromEmptyContent() {
        var paginated = new PaginatedResult<UserBiometriaResult>(List.of(), 0, 10, 0, 0);
        assertThat(UserBiometriaListResponse.from(paginated).content()).isEmpty();
    }
    @Test
    void equalsAndHashCode() {
        assertThat(new UserBiometriaListResponse(List.of(), 0, 0, 0, 0))
                .isEqualTo(new UserBiometriaListResponse(List.of(), 0, 0, 0, 0));
    }
}
