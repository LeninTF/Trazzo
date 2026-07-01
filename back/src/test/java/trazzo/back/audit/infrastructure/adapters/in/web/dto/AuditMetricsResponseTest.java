package trazzo.back.audit.infrastructure.adapters.in.web.dto;

import org.junit.jupiter.api.Test;
import trazzo.back.audit.application.dto.result.AuditMetricsResult;

import static org.assertj.core.api.Assertions.assertThat;

class AuditMetricsResponseTest {

    @Test
    void shouldCreateFromResult() {
        var result = new AuditMetricsResult(100L, 5L, 20L, 15.5, 20.0);

        var response = AuditMetricsResponse.from(result);

        assertThat(response.totalEventos()).isEqualTo(100);
        assertThat(response.errores()).isEqualTo(5);
        assertThat(response.sesionesActivas()).isEqualTo(20);
        assertThat(response.crecimiento()).isEqualTo(15.5);
        assertThat(response.porcentajeSesiones()).isEqualTo(20.0);
    }
}
