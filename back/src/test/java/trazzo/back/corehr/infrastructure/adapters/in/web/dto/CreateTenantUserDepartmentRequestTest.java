package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;

class CreateTenantUserDepartmentRequestTest {
    @Test
    void constructorAndGetters() {
        var start = LocalDate.of(2025, 1, 1);
        var end = LocalDate.of(2025, 12, 31);
        var r = new CreateTenantUserDepartmentRequest(5L, true, start, end);
        assertThat(r.departmentId()).isEqualTo(5L);
        assertThat(r.isPrimary()).isTrue();
        assertThat(r.startDate()).isEqualTo(start);
        assertThat(r.endDate()).isEqualTo(end);
    }
    @Test
    void allowsNullEndDate() {
        var r = new CreateTenantUserDepartmentRequest(1L, false, LocalDate.now(), null);
        assertThat(r.endDate()).isNull();
    }
    @Test
    void equalsAndHashCode() {
        var d = LocalDate.now();
        assertThat(new CreateTenantUserDepartmentRequest(1L, true, d, null))
                .isEqualTo(new CreateTenantUserDepartmentRequest(1L, true, d, null))
                .hasSameHashCodeAs(new CreateTenantUserDepartmentRequest(1L, true, d, null));
    }
}
