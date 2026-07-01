package trazzo.back.corehr.infrastructure.adapters.out.enroll;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class EnrollSessionResponseTest {

    @Test
    void recordConstructorAndAccessors() {
        var expires = LocalDateTime.now().plusSeconds(120);
        var response = new EnrollSessionResponse("token-x", 20L, 7L, 2, "DVC-002", expires);

        assertThat(response.enrollToken()).isEqualTo("token-x");
        assertThat(response.tenantUserId()).isEqualTo(20L);
        assertThat(response.deviceId()).isEqualTo(7L);
        assertThat(response.fingerIndex()).isEqualTo(2);
        assertThat(response.deviceCode()).isEqualTo("DVC-002");
        assertThat(response.expiresAt()).isEqualTo(expires);
    }

    @Test
    void equalsAndHashCode() {
        var expires = LocalDateTime.now().plusSeconds(120);
        var a = new EnrollSessionResponse("t", 1L, 2L, 3, "DVC", expires);
        var b = new EnrollSessionResponse("t", 1L, 2L, 3, "DVC", expires);

        assertThat(a).isEqualTo(b);
        assertThat(a).hasSameHashCodeAs(b);
    }
}
