package trazzo.back.corehr.infrastructure.adapters.out.persistence.entity;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;

class BranchRefEntityTest {

    @Test
    void noArgsConstructor() {
        var entity = new BranchRefEntity();
        assertThat(entity.getId()).isNull();
    }

    @Test
    void allArgsConstructor() {
        var deleted = LocalDateTime.now();
        var entity = new BranchRefEntity(1L, "Main", "Main branch", true, deleted);
        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getName()).isEqualTo("Main");
        assertThat(entity.getDescription()).isEqualTo("Main branch");
        assertThat(entity.getState()).isTrue();
        assertThat(entity.getDeletedAt()).isEqualTo(deleted);
    }

    @Test
    void settersAndGetters() {
        var entity = new BranchRefEntity();
        entity.setId(2L);
        entity.setName("Branch2");
        entity.setDescription("desc");
        entity.setState(false);
        entity.setDeletedAt(null);

        assertThat(entity.getId()).isEqualTo(2L);
        assertThat(entity.getName()).isEqualTo("Branch2");
        assertThat(entity.getState()).isFalse();
        assertThat(entity.getDeletedAt()).isNull();
    }
}
