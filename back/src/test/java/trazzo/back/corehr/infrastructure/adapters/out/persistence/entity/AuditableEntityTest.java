package trazzo.back.corehr.infrastructure.adapters.out.persistence.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AuditableEntityTest {

    private static class TestEntity extends AuditableEntity {
    }

    @Test
    void onCreateSetsBothTimestamps() {
        var entity = new TestEntity();
        entity.onCreate();

        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isNotNull();
        assertThat(entity.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
        assertThat(entity.getUpdatedAt()).isEqualToIgnoringNanos(entity.getCreatedAt());
    }

    @Test
    void onUpdateSetsOnlyUpdatedAt() {
        var entity = new TestEntity();
        entity.setCreatedAt(LocalDateTime.of(2024, 1, 1, 0, 0));
        entity.setUpdatedAt(LocalDateTime.of(2024, 1, 1, 0, 0));
        entity.onUpdate();

        assertThat(entity.getCreatedAt()).isEqualTo(LocalDateTime.of(2024, 1, 1, 0, 0));
        assertThat(entity.getUpdatedAt()).isAfterOrEqualTo(LocalDateTime.of(2024, 1, 1, 0, 0));
    }

    @Test
    void setterAndGetter() {
        var entity = new TestEntity();
        var now = LocalDateTime.of(2025, 6, 1, 10, 0);

        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        assertThat(entity.getCreatedAt()).isEqualTo(now);
        assertThat(entity.getUpdatedAt()).isEqualTo(now);
    }
}
