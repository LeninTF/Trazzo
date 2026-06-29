package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InitEnrollRequestTest {

    @Test
    void constructorAndGetters() {
        var request = new InitEnrollRequest(1L, 2L, 5);

        assertThat(request.tenantUserId()).isEqualTo(1L);
        assertThat(request.deviceId()).isEqualTo(2L);
        assertThat(request.fingerIndex()).isEqualTo(5);
    }

    @Test
    void equalsAndHashCode() {
        var a = new InitEnrollRequest(1L, 2L, 3);
        var b = new InitEnrollRequest(1L, 2L, 3);

        assertThat(a).isEqualTo(b);
        assertThat(a).hasSameHashCodeAs(b);
    }
}
