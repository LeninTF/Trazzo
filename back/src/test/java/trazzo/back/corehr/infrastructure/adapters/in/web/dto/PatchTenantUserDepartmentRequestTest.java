package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;

class PatchTenantUserDepartmentRequestTest {
    @Test
    void constructorAndGetters() {
        var end = LocalDate.of(2025, 12, 31);
        var r = new PatchTenantUserDepartmentRequest(end, false);
        assertThat(r.endDate()).isEqualTo(end);
        assertThat(r.isPrimary()).isFalse();
    }
    @Test
    void allowsAllNull() {
        var r = new PatchTenantUserDepartmentRequest(null, null);
        assertThat(r.endDate()).isNull();
        assertThat(r.isPrimary()).isNull();
    }
    @Test
    void equalsAndHashCode() {
        assertThat(new PatchTenantUserDepartmentRequest(null, null))
                .isEqualTo(new PatchTenantUserDepartmentRequest(null, null));
    }
}
