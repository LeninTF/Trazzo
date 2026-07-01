package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import org.junit.jupiter.api.Test;
import trazzo.back.corehr.application.dto.result.PaginatedResult;
import trazzo.back.corehr.application.dto.result.ToleranciaResult;
import trazzo.back.corehr.domain.model.ToleranciaType;
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class ToleranciaListResponseTest {
    @Test
    void fromMapsPaginatedResult() {
        var now = LocalDateTime.now();
        var result = new ToleranciaResult(1L, 10L, "T1", ToleranciaType.ENTRADA, 15, "desc", true, now, now);
        var paginated = new PaginatedResult<>(List.of(result), 1, 10, 25, 3);
        var response = ToleranciaListResponse.from(paginated);
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).name()).isEqualTo("T1");
    }
    @Test
    void fromEmptyContent() {
        var paginated = new PaginatedResult<ToleranciaResult>(List.of(), 0, 10, 0, 0);
        assertThat(ToleranciaListResponse.from(paginated).content()).isEmpty();
    }
    @Test
    void equalsAndHashCode() {
        assertThat(new ToleranciaListResponse(List.of(), 0, 0, 0, 0))
                .isEqualTo(new ToleranciaListResponse(List.of(), 0, 0, 0, 0));
    }
}
