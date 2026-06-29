package trazzo.back.corehr.infrastructure.adapters.out.persistence.entity;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;

class UserBiometriaEntityTest {

    @Test
    void noArgsConstructor() {
        var entity = new UserBiometriaEntity();
        assertThat(entity.getId()).isNull();
    }

    @Test
    void allArgsConstructor() {
        var now = LocalDateTime.now();
        var entity = new UserBiometriaEntity(1L, 10L, 5L, 3, "tmpl", "key", now, true);
        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getTenantUserId()).isEqualTo(10L);
        assertThat(entity.getDeviceId()).isEqualTo(5L);
        assertThat(entity.getFingerIndex()).isEqualTo(3);
        assertThat(entity.getTemplateCifrado()).isEqualTo("tmpl");
        assertThat(entity.getLlaveCifrado()).isEqualTo("key");
        assertThat(entity.getCapturadoEn()).isEqualTo(now);
        assertThat(entity.isActivo()).isTrue();
    }

    @Test
    void settersAndGetters() {
        var entity = new UserBiometriaEntity();
        entity.setId(2L);
        entity.setTenantUserId(20L);
        entity.setDeviceId(null);
        entity.setFingerIndex(null);
        entity.setTemplateCifrado("tmpl2");
        entity.setLlaveCifrado(null);
        entity.setCapturadoEn(null);
        entity.setActivo(false);

        assertThat(entity.getId()).isEqualTo(2L);
        assertThat(entity.getDeviceId()).isNull();
        assertThat(entity.getFingerIndex()).isNull();
        assertThat(entity.getLlaveCifrado()).isNull();
        assertThat(entity.getCapturadoEn()).isNull();
        assertThat(entity.isActivo()).isFalse();
    }
}
