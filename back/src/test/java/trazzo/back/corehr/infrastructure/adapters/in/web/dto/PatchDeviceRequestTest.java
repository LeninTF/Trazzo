package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PatchDeviceRequestTest {
    @Test
    void constructorAndGetters() {
        var r = new PatchDeviceRequest("Device1", 10L, "192.168.1.1", 8080, "Office", true);
        assertThat(r.name()).isEqualTo("Device1");
        assertThat(r.branchId()).isEqualTo(10L);
        assertThat(r.ip()).isEqualTo("192.168.1.1");
        assertThat(r.puerto()).isEqualTo(8080);
        assertThat(r.ubicacion()).isEqualTo("Office");
        assertThat(r.state()).isTrue();
    }
    @Test
    void allowsAllNull() {
        var r = new PatchDeviceRequest(null, null, null, null, null, null);
        assertThat(r.name()).isNull();
        assertThat(r.state()).isNull();
    }
    @Test
    void equalsAndHashCode() {
        assertThat(new PatchDeviceRequest(null, null, null, null, null, null))
                .isEqualTo(new PatchDeviceRequest(null, null, null, null, null, null));
    }
}
