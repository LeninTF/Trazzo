package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import org.junit.jupiter.api.Test;
import trazzo.back.corehr.infrastructure.adapters.out.enroll.EnrollSession;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PendingEnrollSessionResponseTest {

    @Test
    void from_shouldMapAllFields() {
        var now = LocalDateTime.now();
        var session = new EnrollSession("token-1", 10L, 5L, 3, "DVC-001", now);

        var response = PendingEnrollSessionResponse.from(session);

        assertThat(response.enrollToken()).isEqualTo("token-1");
        assertThat(response.deviceId()).isEqualTo(5L);
        assertThat(response.deviceCode()).isEqualTo("DVC-001");
        assertThat(response.tenantUserId()).isEqualTo(10L);
        assertThat(response.fingerIndex()).isEqualTo(3);
        assertThat(response.expiresAt()).isEqualTo(now);
    }
}
