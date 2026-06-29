package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PatchToleranciaRequestTest {
    @Test
    void constructorAndGetters() {
        var r = new PatchToleranciaRequest("T1", 15, "desc", true);
        assertThat(r.name()).isEqualTo("T1");
        assertThat(r.minutes()).isEqualTo(15);
        assertThat(r.description()).isEqualTo("desc");
        assertThat(r.activo()).isTrue();
    }
    @Test
    void allowsAllNull() {
        var r = new PatchToleranciaRequest(null, null, null, null);
        assertThat(r.name()).isNull();
        assertThat(r.activo()).isNull();
    }
    @Test
    void equalsAndHashCode() {
        assertThat(new PatchToleranciaRequest(null, null, null, null))
                .isEqualTo(new PatchToleranciaRequest(null, null, null, null));
    }
}
