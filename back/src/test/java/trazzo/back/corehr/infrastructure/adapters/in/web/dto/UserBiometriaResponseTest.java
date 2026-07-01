package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import org.junit.jupiter.api.Test;
import trazzo.back.corehr.application.dto.result.UserBiometriaResult;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;

class UserBiometriaResponseTest {

    @Test
    void fromMapsAllFields() {
        var now = LocalDateTime.now();
        var result = new UserBiometriaResult(1L, 10L, 5L, "DVC-001", 3, true, now, now, now);
        var response = UserBiometriaResponse.from(result);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.tenantUserId()).isEqualTo(10L);
        assertThat(response.deviceId()).isEqualTo(5L);
        assertThat(response.deviceCode()).isEqualTo("DVC-001");
        assertThat(response.fingerIndex()).isEqualTo(3);
        assertThat(response.activo()).isTrue();
        assertThat(response.capturadoEn()).isEqualTo(now);
    }

    @Test
    void fromHandlesNullOptionals() {
        var now = LocalDateTime.now();
        var result = new UserBiometriaResult(1L, 10L, null, null, null, false, null, now, now);
        var response = UserBiometriaResponse.from(result);
        assertThat(response.deviceId()).isNull();
        assertThat(response.deviceCode()).isNull();
        assertThat(response.fingerIndex()).isNull();
        assertThat(response.capturadoEn()).isNull();
    }

    @Test
    void equalsAndHashCode() {
        var now = LocalDateTime.now();
        var a = new UserBiometriaResponse(1L, 10L, null, null, null, false, null, now, now);
        var b = new UserBiometriaResponse(1L, 10L, null, null, null, false, null, now, now);
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }
}
