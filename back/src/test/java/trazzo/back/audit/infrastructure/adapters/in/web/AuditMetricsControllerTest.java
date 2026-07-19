package trazzo.back.audit.infrastructure.adapters.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import trazzo.back.audit.application.dto.result.AuditMetricsResult;
import trazzo.back.audit.application.port.in.AuditMetricsUseCase;
import trazzo.back.shared.security.AuthenticatedUser;
import trazzo.back.shared.security.SecurityConfig;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuditMetricsController.class)
@Import(SecurityConfig.class)
class AuditMetricsControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AuditMetricsUseCase auditMetricsUseCase;

    private static AuditMetricsResult aResult() {
        return new AuditMetricsResult(100L, 5L, 20L, 15.5, 20.0);
    }

    private static UsernamePasswordAuthenticationToken authWithAuthorities(String... authorities) {
        var authUser = new AuthenticatedUser(UUID.randomUUID(), "admin@trazzo.pe", "pass", List.of(), true);
        var grants = List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList();
        return new UsernamePasswordAuthenticationToken(authUser, null, grants);
    }

    @Test
    void getMetrics_shouldReturn200WithAuditMetricsResponse() throws Exception {
        when(auditMetricsUseCase.getMetrics()).thenReturn(aResult());

        mockMvc.perform(get("/audit/metrics")
                        .with(authentication(authWithAuthorities("ROLE_SAAS_ADMIN", "monitoreo-sistema.dashboard-global"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_eventos").value(100))
                .andExpect(jsonPath("$.errores").value(5))
                .andExpect(jsonPath("$.sesiones_activas").value(20))
                .andExpect(jsonPath("$.crecimiento").value(15.5))
                .andExpect(jsonPath("$.porcentaje_sesiones").value(20.0));
    }

    @Test
    void getMetrics_returns403_whenUserLacksDashboardPermission() throws Exception {
        mockMvc.perform(get("/audit/metrics")
                        .with(authentication(authWithAuthorities("ROLE_SAAS_ADMIN"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMetrics_returns401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/audit/metrics"))
                .andExpect(status().isUnauthorized());
    }
}
