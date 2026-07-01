package trazzo.back.corehr.infrastructure.adapters.out.persistence.entity;

import org.junit.jupiter.api.Test;
import java.time.LocalTime;
import static org.assertj.core.api.Assertions.assertThat;

class UserScheduleEntityTest {

    @Test
    void noArgsConstructor() {
        var entity = new UserScheduleEntity();
        assertThat(entity.getId()).isNull();
    }

    @Test
    void allArgsConstructor() {
        var entry = LocalTime.of(8, 0);
        var dep = LocalTime.of(17, 0);
        var entity = new UserScheduleEntity(1L, 10L, 5L, "desc", entry, dep);
        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getTenantUserId()).isEqualTo(10L);
        assertThat(entity.getScheduleId()).isEqualTo(5L);
        assertThat(entity.getDescription()).isEqualTo("desc");
        assertThat(entity.getEntryTime()).isEqualTo(entry);
        assertThat(entity.getDepartureTime()).isEqualTo(dep);
    }

    @Test
    void settersAndGetters() {
        var entity = new UserScheduleEntity();
        entity.setId(2L);
        entity.setTenantUserId(20L);
        entity.setScheduleId(10L);
        entity.setDescription(null);
        entity.setEntryTime(LocalTime.of(9, 0));
        entity.setDepartureTime(LocalTime.of(18, 0));

        assertThat(entity.getId()).isEqualTo(2L);
        assertThat(entity.getDescription()).isNull();
        assertThat(entity.getEntryTime()).isEqualTo(LocalTime.of(9, 0));
    }
}
