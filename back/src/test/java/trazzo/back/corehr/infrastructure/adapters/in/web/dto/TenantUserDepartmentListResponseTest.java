package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import org.junit.jupiter.api.Test;
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
        var response = TenantUserDepartmentListResponse.from(List.of(result));
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).departmentName()).isEqualTo("IT");
    }
    @Test
    void fromEmptyList() {
        var response = TenantUserDepartmentListResponse.from(List.of());
        assertThat(response.content()).isEmpty();
    }
    @Test
    void equalsAndHashCode() {
        assertThat(new TenantUserDepartmentListResponse(List.of()))
                .isEqualTo(new TenantUserDepartmentListResponse(List.of()));
    }
}
