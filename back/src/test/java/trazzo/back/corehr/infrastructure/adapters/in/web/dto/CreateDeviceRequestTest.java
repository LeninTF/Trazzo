package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CreateDeviceRequestTest {
    @Test
    void constructorAndGetters() {
        var r = new CreateDeviceRequest("D-001", "Device1", 10L, "192.168.1.1", 8080, "Office");
        assertThat(r.code()).isEqualTo("D-001");
        assertThat(r.name()).isEqualTo("Device1");
        assertThat(r.branchId()).isEqualTo(10L);
        assertThat(r.ip()).isEqualTo("192.168.1.1");
        assertThat(r.puerto()).isEqualTo(8080);
        assertThat(r.ubicacion()).isEqualTo("Office");
    }
    @Test
    void allowsNullOptionals() {
        var r = new CreateDeviceRequest("D-002", null, 20L, null, null, null);
        assertThat(r.name()).isNull();
        assertThat(r.ip()).isNull();
    }
    @Test
    void equalsAndHashCode() {
        var a = new CreateDeviceRequest("c", null, 1L, null, null, null);
        var b = new CreateDeviceRequest("c", null, 1L, null, null, null);
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }
}
