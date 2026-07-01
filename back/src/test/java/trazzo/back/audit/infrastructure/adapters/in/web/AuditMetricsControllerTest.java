package trazzo.back.audit.infrastructure.adapters.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import trazzo.back.audit.application.dto.result.AuditMetricsResult;
import trazzo.back.audit.application.port.in.AuditMetricsUseCase;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuditMetricsController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuditMetricsControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AuditMetricsUseCase auditMetricsUseCase;

    private static AuditMetricsResult aResult() {
        return new AuditMetricsResult(100L, 5L, 20L, 15.5, 20.0);
    }

    @Test
    void getMetrics_shouldReturn200WithAuditMetricsResponse() throws Exception {
        when(auditMetricsUseCase.getMetrics()).thenReturn(aResult());

        mockMvc.perform(get("/audit/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_eventos").value(100))
                .andExpect(jsonPath("$.errores").value(5))
                .andExpect(jsonPath("$.sesiones_activas").value(20))
                .andExpect(jsonPath("$.crecimiento").value(15.5))
                .andExpect(jsonPath("$.porcentaje_sesiones").value(20.0));
    }
}
