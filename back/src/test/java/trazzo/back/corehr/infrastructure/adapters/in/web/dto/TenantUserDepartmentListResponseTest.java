package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import org.junit.jupiter.api.Test;
import trazzo.back.corehr.application.dto.result.PaginatedResult;
import trazzo.back.corehr.application.dto.result.TenantUserDepartmentResult;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class TenantUserDepartmentListResponseTest {
    @Test
    void fromMapsListOfResults() {
        var now = LocalDateTime.now();
        var result = new TenantUserDepartmentResult(1L, 10L, 5L, "IT", true,
                LocalDate.of(2025, 1, 1), null, now, now);
        var paginated = new PaginatedResult<TenantUserDepartmentResult>(List.of(result), 0, 10, 1, 1);
        var response = TenantUserDepartmentListResponse.from(paginated);
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).departmentName()).isEqualTo("IT");
    }
    @Test
    void fromEmptyList() {
        var paginated = new PaginatedResult<TenantUserDepartmentResult>(List.of(), 0, 10, 0, 0);
        var response = TenantUserDepartmentListResponse.from(paginated);
        assertThat(response.content()).isEmpty();
    }
    @Test
    void equalsAndHashCode() {
        assertThat(new TenantUserDepartmentListResponse(List.of(), 0, 10, 0, 0))
                .isEqualTo(new TenantUserDepartmentListResponse(List.of(), 0, 10, 0, 0));
    }
}
