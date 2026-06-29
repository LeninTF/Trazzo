package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;

class EnrollSessionResponseTest {

    @Test
    void fromMapsFromOutboundResponse() {
        var expires = LocalDateTime.now().plusSeconds(120);
        var outbound = new trazzo.back.corehr.infrastructure.adapters.out.enroll.EnrollSessionResponse(
                "token-1", 10L, 5L, 3, "DVC-001", expires);
        var response = EnrollSessionResponse.from(outbound);

        assertThat(response.enrollToken()).isEqualTo("token-1");
        assertThat(response.tenantUserId()).isEqualTo(10L);
        assertThat(response.deviceId()).isEqualTo(5L);
        assertThat(response.fingerIndex()).isEqualTo(3);
        assertThat(response.deviceCode()).isEqualTo("DVC-001");
        assertThat(response.expiresAt()).isEqualTo(expires);
    }

    @Test
    void equalsAndHashCode() {
        var expires = LocalDateTime.now().plusSeconds(120);
        var a = new EnrollSessionResponse("t", 1L, 2L, 3, "DVC", expires);
        var b = new EnrollSessionResponse("t", 1L, 2L, 3, "DVC", expires);
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }
}
