package trazzo.back.corehr.infrastructure.adapters.out.persistence.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class NonWorkingDaysEntityTest {

    @Test
    void noArgsConstructor() {
        var entity = new NonWorkingDaysEntity();

        assertThat(entity.getId()).isNull();
        assertThat(entity.getDate()).isNull();
    }

    @Test
    void allArgsConstructor() {
        var now = LocalDateTime.now();
        var date = LocalDate.of(2025, 12, 25);
        var entity = new NonWorkingDaysEntity(1L, date, "Christmas", true, now);

        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getDate()).isEqualTo(date);
        assertThat(entity.getDescription()).isEqualTo("Christmas");
        assertThat(entity.isRecurring()).isTrue();
        assertThat(entity.getCreatedAt()).isEqualTo(now);
    }

    @Test
    void settersAndGetters() {
        var entity = new NonWorkingDaysEntity();

        entity.setId(2L);
        entity.setDate(LocalDate.of(2025, 1, 1));
        entity.setDescription("New Year");
        entity.setRecurring(false);

        assertThat(entity.getId()).isEqualTo(2L);
        assertThat(entity.getDate()).isEqualTo(LocalDate.of(2025, 1, 1));
        assertThat(entity.getDescription()).isEqualTo("New Year");
        assertThat(entity.isRecurring()).isFalse();
    }

    @Test
    void onCreateSetsCreatedAt() {
        var entity = new NonWorkingDaysEntity();
        entity.onCreate();

        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }
}
