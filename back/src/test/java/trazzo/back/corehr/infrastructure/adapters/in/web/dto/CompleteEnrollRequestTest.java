package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CompleteEnrollRequestTest {

    @Test
    void constructorAndGetters() {
        var capturado = LocalDateTime.now();
        var request = new CompleteEnrollRequest("token-1", "tmpl", "key", 3, "DVC-001", capturado);

        assertThat(request.enrollToken()).isEqualTo("token-1");
        assertThat(request.templateCifrado()).isEqualTo("tmpl");
        assertThat(request.llaveCifrado()).isEqualTo("key");
        assertThat(request.fingerIndex()).isEqualTo(3);
        assertThat(request.deviceCode()).isEqualTo("DVC-001");
        assertThat(request.capturadoEn()).isEqualTo(capturado);
    }

    @Test
    void allowsNullCapturadoEn() {
        var request = new CompleteEnrollRequest("token", "tmpl", "key", 0, "DVC", null);

        assertThat(request.capturadoEn()).isNull();
    }

    @Test
    void equalsAndHashCode() {
        var capturado = LocalDateTime.now();
        var a = new CompleteEnrollRequest("t", "tmpl", "key", 1, "DVC", capturado);
        var b = new CompleteEnrollRequest("t", "tmpl", "key", 1, "DVC", capturado);

        assertThat(a).isEqualTo(b);
        assertThat(a).hasSameHashCodeAs(b);
    }
}
