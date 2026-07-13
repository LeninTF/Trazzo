package trazzo.back.saasglobal.infrastructure.adapters.in.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import trazzo.back.saasglobal.application.dto.result.PlanResult;
import trazzo.back.saasglobal.application.port.in.PlanUseCase;
import trazzo.back.shared.security.SecurityConfig;

@WebMvcTest(PublicPlanController.class)
@Import(SecurityConfig.class)
class PublicPlanControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean PlanUseCase planUseCase;

    @Test
    void listActive_withoutAuthentication_returns200() throws Exception {
        when(planUseCase.listActive()).thenReturn(List.of(
                new PlanResult(1, "Plan Demo", BigDecimal.valueOf(49), BigDecimal.valueOf(490),
                        "SOLES", "MONTHLY", true, LocalDateTime.now(), Map.of("max_trabajadores", 5))));

        mockMvc.perform(get("/public/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Plan Demo"))
                .andExpect(jsonPath("$[0].price").value(49));
    }

    @Test
    void listActive_returnsEmptyWhenNoActivePlans() throws Exception {
        when(planUseCase.listActive()).thenReturn(List.of());

        mockMvc.perform(get("/public/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}
