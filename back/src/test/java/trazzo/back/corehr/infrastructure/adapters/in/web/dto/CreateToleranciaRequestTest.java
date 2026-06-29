package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import org.junit.jupiter.api.Test;
import trazzo.back.corehr.domain.model.ToleranciaType;
import static org.assertj.core.api.Assertions.assertThat;

class CreateToleranciaRequestTest {
    @Test
    void constructorAndGetters() {
        var r = new CreateToleranciaRequest(ToleranciaType.ENTRADA, 15, "T1", "desc");
        assertThat(r.type()).isEqualTo(ToleranciaType.ENTRADA);
        assertThat(r.minutes()).isEqualTo(15);
        assertThat(r.name()).isEqualTo("T1");
        assertThat(r.description()).isEqualTo("desc");
    }
    @Test
    void allowsNullNameAndDescription() {
        var r = new CreateToleranciaRequest(ToleranciaType.ENTRADA, 0, null, null);
        assertThat(r.name()).isNull();
        assertThat(r.description()).isNull();
    }
    @Test
    void equalsAndHashCode() {
        assertThat(new CreateToleranciaRequest(ToleranciaType.ENTRADA, 5, "n", "d"))
                .isEqualTo(new CreateToleranciaRequest(ToleranciaType.ENTRADA, 5, "n", "d"))
                .hasSameHashCodeAs(new CreateToleranciaRequest(ToleranciaType.ENTRADA, 5, "n", "d"));
    }
}
