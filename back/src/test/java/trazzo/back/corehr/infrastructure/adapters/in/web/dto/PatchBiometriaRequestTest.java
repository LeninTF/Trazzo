package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PatchBiometriaRequestTest {
    @Test
    void constructorAndGetters() {
        var r = new PatchBiometriaRequest(true);
        assertThat(r.activo()).isTrue();
    }
    @Test
    void equalsAndHashCode() {
        assertThat(new PatchBiometriaRequest(true)).isEqualTo(new PatchBiometriaRequest(true));
        assertThat(new PatchBiometriaRequest(false)).isEqualTo(new PatchBiometriaRequest(false));
    }
}
