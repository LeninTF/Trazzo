package trazzo.back.corehr.infrastructure.adapters.out.persistence.entity;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;

class TenantContactEntityTest {

    @Test
    void noArgsConstructor() {
        var entity = new TenantContactEntity();
        assertThat(entity.getId()).isNull();
    }

    @Test
    void allArgsConstructor() {
        var deleted = LocalDateTime.now();
        var entity = new TenantContactEntity(1L, 10L, "email", deleted);
        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getTenantUserId()).isEqualTo(10L);
        assertThat(entity.getType()).isEqualTo("email");
        assertThat(entity.getDeletedAt()).isEqualTo(deleted);
    }

    @Test
    void settersAndGetters() {
        var entity = new TenantContactEntity();
        entity.setId(2L);
        entity.setTenantUserId(20L);
        entity.setType("phone");
        entity.setDeletedAt(null);

        assertThat(entity.getId()).isEqualTo(2L);
        assertThat(entity.getTenantUserId()).isEqualTo(20L);
        assertThat(entity.getType()).isEqualTo("phone");
        assertThat(entity.getDeletedAt()).isNull();
    }
}
