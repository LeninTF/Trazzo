package trazzo.back.corehr.infrastructure.adapters.out.enroll;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class EnrollSessionTest {

    @Test
    void recordConstructorAndAccessors() {
        var expires = LocalDateTime.now().plusSeconds(120);
        var session = new EnrollSession("token-1", 10L, 5L, 3, "DVC-001", expires);

        assertThat(session.enrollToken()).isEqualTo("token-1");
        assertThat(session.tenantUserId()).isEqualTo(10L);
        assertThat(session.deviceId()).isEqualTo(5L);
        assertThat(session.fingerIndex()).isEqualTo(3);
        assertThat(session.deviceCode()).isEqualTo("DVC-001");
        assertThat(session.expiresAt()).isEqualTo(expires);
    }

    @Test
    void equalsAndHashCode() {
        var expires = LocalDateTime.now().plusSeconds(120);
        var a = new EnrollSession("t", 1L, 2L, 3, "DVC", expires);
        var b = new EnrollSession("t", 1L, 2L, 3, "DVC", expires);

        assertThat(a).isEqualTo(b);
        assertThat(a).hasSameHashCodeAs(b);
    }

    @Test
    void differentValuesNotEqual() {
        var expires = LocalDateTime.now().plusSeconds(120);
        var a = new EnrollSession("t1", 1L, 2L, 3, "DVC", expires);
        var b = new EnrollSession("t2", 1L, 2L, 3, "DVC", expires);

        assertThat(a).isNotEqualTo(b);
    }
}
