package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PatchShiftRequestTest {
    @Test
    void constructorAndGetters() {
        var r = new PatchShiftRequest("Morning", "desc");
        assertThat(r.name()).isEqualTo("Morning");
        assertThat(r.description()).isEqualTo("desc");
    }
    @Test
    void allowsAllNull() {
        var r = new PatchShiftRequest(null, null);
        assertThat(r.name()).isNull();
    }
    @Test
    void equalsAndHashCode() {
        assertThat(new PatchShiftRequest("n", "d")).isEqualTo(new PatchShiftRequest("n", "d"));
        assertThat(new PatchShiftRequest(null, null)).isEqualTo(new PatchShiftRequest(null, null));
    }
}
