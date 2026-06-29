package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import org.junit.jupiter.api.Test;
import trazzo.back.corehr.application.dto.result.PaginatedResult;
import trazzo.back.corehr.application.dto.result.TenantContactResult;
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class TenantContactListResponseTest {
    @Test
    void fromMapsPaginatedResult() {
        var now = LocalDateTime.now();
        var result = new TenantContactResult(1L, 10L, "email", null, now, now, null);
        var paginated = new PaginatedResult<>(List.of(result), 2, 5, 15, 3);
        var response = TenantContactListResponse.from(paginated);
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).type()).isEqualTo("email");
        assertThat(response.page()).isEqualTo(2);
    }
    @Test
    void fromEmptyContent() {
        var paginated = new PaginatedResult<TenantContactResult>(List.of(), 0, 10, 0, 0);
        assertThat(TenantContactListResponse.from(paginated).content()).isEmpty();
    }
    @Test
    void equalsAndHashCode() {
        assertThat(new TenantContactListResponse(List.of(), 0, 0, 0, 0))
                .isEqualTo(new TenantContactListResponse(List.of(), 0, 0, 0, 0));
    }
}
