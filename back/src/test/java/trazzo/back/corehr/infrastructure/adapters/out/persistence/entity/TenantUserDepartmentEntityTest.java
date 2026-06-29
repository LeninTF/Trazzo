package trazzo.back.corehr.infrastructure.adapters.out.persistence.entity;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;

class TenantUserDepartmentEntityTest {

    @Test
    void noArgsConstructor() {
        var entity = new TenantUserDepartmentEntity();
        assertThat(entity.getId()).isNull();
    }

    @Test
    void allArgsConstructor() {
        var start = LocalDate.of(2025, 1, 1);
        var end = LocalDate.of(2025, 12, 31);
        var entity = new TenantUserDepartmentEntity(1L, 10L, 5L, true, start, end);
        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getTenantUserId()).isEqualTo(10L);
        assertThat(entity.getDepartmentId()).isEqualTo(5L);
        assertThat(entity.isPrimary()).isTrue();
        assertThat(entity.getStartDate()).isEqualTo(start);
        assertThat(entity.getEndDate()).isEqualTo(end);
    }

    @Test
    void settersAndGetters() {
        var entity = new TenantUserDepartmentEntity();
        entity.setId(2L);
        entity.setTenantUserId(20L);
        entity.setDepartmentId(10L);
        entity.setPrimary(false);
        entity.setStartDate(LocalDate.of(2025, 6, 1));
        entity.setEndDate(null);

        assertThat(entity.getId()).isEqualTo(2L);
        assertThat(entity.isPrimary()).isFalse();
        assertThat(entity.getEndDate()).isNull();
    }
}
