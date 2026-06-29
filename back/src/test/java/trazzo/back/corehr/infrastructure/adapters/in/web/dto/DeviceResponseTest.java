package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import org.junit.jupiter.api.Test;
import trazzo.back.corehr.application.dto.result.DeviceResult;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceResponseTest {

    @Test
    void fromMapsAllFields() {
        var now = LocalDateTime.now();
        var result = new DeviceResult(1L, "D-001", "Device1", 10L, "Branch1",
                "192.168.1.1", 8080, "Office", true, now);

        var response = DeviceResponse.from(result);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.code()).isEqualTo("D-001");
        assertThat(response.name()).isEqualTo("Device1");
        assertThat(response.branchId()).isEqualTo(10L);
        assertThat(response.branchName()).isEqualTo("Branch1");
        assertThat(response.ip()).isEqualTo("192.168.1.1");
        assertThat(response.puerto()).isEqualTo(8080);
        assertThat(response.ubicacion()).isEqualTo("Office");
        assertThat(response.state()).isTrue();
        assertThat(response.createdAt()).isEqualTo(now);
    }

    @Test
    void fromWithNullOptionals() {
        var now = LocalDateTime.now();
        var result = new DeviceResult(2L, "D-002", null, null, null, null, null, null, false, now);

        var response = DeviceResponse.from(result);

        assertThat(response.name()).isNull();
        assertThat(response.branchId()).isNull();
        assertThat(response.ip()).isNull();
        assertThat(response.puerto()).isNull();
        assertThat(response.ubicacion()).isNull();
        assertThat(response.state()).isFalse();
    }

    @Test
    void equalsAndHashCode() {
        var now = LocalDateTime.now();
        var a = new DeviceResponse(1L, "c", "n", null, null, null, null, null, false, now);
        var b = new DeviceResponse(1L, "c", "n", null, null, null, null, null, false, now);

        assertThat(a).isEqualTo(b);
        assertThat(a).hasSameHashCodeAs(b);
    }
}
