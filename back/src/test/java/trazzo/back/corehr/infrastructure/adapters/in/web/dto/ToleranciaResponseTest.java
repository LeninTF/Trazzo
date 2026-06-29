package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import org.junit.jupiter.api.Test;
import trazzo.back.corehr.application.dto.result.ToleranciaResult;
import trazzo.back.corehr.domain.model.ToleranciaType;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;

class ToleranciaResponseTest {

    @Test
    void fromMapsAllFields() {
        var now = LocalDateTime.now();
        var result = new ToleranciaResult(1L, 10L, "T1", ToleranciaType.ENTRADA, 15, "desc", true, now, now);
        var response = ToleranciaResponse.from(result);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.scheduleId()).isEqualTo(10L);
        assertThat(response.name()).isEqualTo("T1");
        assertThat(response.type()).isEqualTo(ToleranciaType.ENTRADA);
        assertThat(response.minutes()).isEqualTo(15);
        assertThat(response.description()).isEqualTo("desc");
        assertThat(response.activo()).isTrue();
    }

    @Test
    void fromHandlesNullFields() {
        var now = LocalDateTime.now();
        var result = new ToleranciaResult(1L, null, null, null, null, null, false, now, now);
        var response = ToleranciaResponse.from(result);
        assertThat(response.scheduleId()).isNull();
        assertThat(response.name()).isNull();
        assertThat(response.minutes()).isNull();
    }

    @Test
    void equalsAndHashCode() {
        var now = LocalDateTime.now();
        var a = new ToleranciaResponse(1L, 10L, "n", ToleranciaType.ENTRADA, 5, "d", true, now, now);
        var b = new ToleranciaResponse(1L, 10L, "n", ToleranciaType.ENTRADA, 5, "d", true, now, now);
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }
}
