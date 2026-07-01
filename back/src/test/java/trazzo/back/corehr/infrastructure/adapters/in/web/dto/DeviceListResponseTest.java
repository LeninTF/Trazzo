package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import org.junit.jupiter.api.Test;
import trazzo.back.corehr.application.dto.result.DeviceResult;
import trazzo.back.corehr.application.dto.result.PaginatedResult;
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class DeviceListResponseTest {
    @Test
    void fromMapsPaginatedResult() {
        var now = LocalDateTime.now();
        var result = new DeviceResult(1L, "D-001", "Device1", 10L, "Branch1", null, null, null, true, now);
        var paginated = new PaginatedResult<>(List.of(result), 0, 10, 1, 1);
        var response = DeviceListResponse.from(paginated);
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).code()).isEqualTo("D-001");
        assertThat(response.page()).isZero();
        assertThat(response.totalElements()).isEqualTo(1);
    }
    @Test
    void fromWithEmptyContent() {
        var paginated = new PaginatedResult<DeviceResult>(List.of(), 0, 10, 0, 0);
        assertThat(DeviceListResponse.from(paginated).content()).isEmpty();
    }
    @Test
    void equalsAndHashCode() {
        assertThat(new DeviceListResponse(List.of(), 0, 0, 0, 0))
                .isEqualTo(new DeviceListResponse(List.of(), 0, 0, 0, 0));
    }
}
