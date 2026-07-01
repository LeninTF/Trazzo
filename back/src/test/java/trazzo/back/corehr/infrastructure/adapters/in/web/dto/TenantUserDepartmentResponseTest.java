package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import org.junit.jupiter.api.Test;
import trazzo.back.corehr.application.dto.result.TenantUserDepartmentResult;
import java.time.LocalDate;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;

class TenantUserDepartmentResponseTest {

    @Test
    void fromMapsAllFields() {
        var now = LocalDateTime.now();
        var start = LocalDate.of(2025, 1, 1);
        var end = LocalDate.of(2025, 12, 31);
        var result = new TenantUserDepartmentResult(1L, 10L, 5L, "IT", true, start, end, now, now);
        var response = TenantUserDepartmentResponse.from(result);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.tenantUserId()).isEqualTo(10L);
        assertThat(response.departmentId()).isEqualTo(5L);
        assertThat(response.departmentName()).isEqualTo("IT");
        assertThat(response.isPrimary()).isTrue();
        assertThat(response.startDate()).isEqualTo(start);
        assertThat(response.endDate()).isEqualTo(end);
    }

    @Test
    void fromAllowsNullEndDate() {
        var now = LocalDateTime.now();
        var result = new TenantUserDepartmentResult(1L, 10L, 5L, "IT", false, LocalDate.now(), null, now, now);
        var response = TenantUserDepartmentResponse.from(result);
        assertThat(response.endDate()).isNull();
    }

    @Test
    void equalsAndHashCode() {
        var now = LocalDateTime.now();
        var d = LocalDate.now();
        var a = new TenantUserDepartmentResponse(1L, 10L, 5L, "IT", true, d, null, now, now);
        var b = new TenantUserDepartmentResponse(1L, 10L, 5L, "IT", true, d, null, now, now);
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }
}
