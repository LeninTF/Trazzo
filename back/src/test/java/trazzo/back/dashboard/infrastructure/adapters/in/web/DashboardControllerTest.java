package trazzo.back.dashboard.infrastructure.adapters.in.web;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import trazzo.back.dashboard.application.port.in.DashboardAlertsUseCase;
import trazzo.back.dashboard.application.port.in.DashboardSummaryUseCase;
import trazzo.back.dashboard.application.port.in.PuntualidadRolUseCase;
import trazzo.back.dashboard.domain.model.AlertaDashboard;
import trazzo.back.dashboard.domain.model.DashboardSummary;
import trazzo.back.dashboard.domain.model.MetricasAsistencia;
import trazzo.back.dashboard.domain.model.PuntualidadPorRol;
import trazzo.back.shared.security.AuthenticatedUser;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@ExtendWith(SpringExtension.class)
@WebMvcTest(DashboardController.class)
@EnableMethodSecurity
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardSummaryUseCase summaryUseCase;

    @MockitoBean
    private PuntualidadRolUseCase puntualidadRolUseCase;

    @MockitoBean
    private DashboardAlertsUseCase alertsUseCase;

    private AuthenticatedUser testUser;

    @BeforeEach
    void setUp() {
        testUser = new AuthenticatedUser(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                "test@mail.com", "pass",
                List.of(new SimpleGrantedAuthority("ROLE_USER"),
                        new SimpleGrantedAuthority("dashboard.ver")), true);
        var auth = new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getSummaryReturns200() throws Exception {
        var summary = new DashboardSummary(20, 90.0,
                new MetricasAsistencia(5, 3), 87.5);
        when(summaryUseCase.getSummary(any(), any())).thenReturn(summary);

        mockMvc.perform(get("/dashboard/summary").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarios_activos").value(20))
                .andExpect(jsonPath("$.indice_puntualidad_anual").value(87.5))
                .andExpect(jsonPath("$.metricas.total_inasistencias").value(5))
                .andExpect(jsonPath("$.metricas.total_incidencias").value(3));
    }

    @Test
    void getPuntualidadPorRolReturns200() throws Exception {
        var model = new PuntualidadPorRol("mes", "2026-07-01", "2026-07-31",
                List.of(new PuntualidadPorRol.RolPuntualidad("Director", 95.0)));
        when(puntualidadRolUseCase.getPuntualidadPorRol(any(), any(), any())).thenReturn(model);

        mockMvc.perform(get("/dashboard/puntualidad-por-rol").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodo").value("mes"))
                .andExpect(jsonPath("$.roles[0].nombre").value("Director"))
                .andExpect(jsonPath("$.roles[0].porcentaje").value(95.0));
    }

    @Test
    void getAlertasReturns200() throws Exception {
        var model = new AlertaDashboard(List.of(
                new AlertaDashboard.Alerta("bi-clock-fill", "Tardanzas", "3 empleados",
                        LocalDateTime.now(), "danger")));
        when(alertsUseCase.getAlertas()).thenReturn(model);

        mockMvc.perform(get("/dashboard/alertas").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alertas[0].titulo").value("Tardanzas"))
                .andExpect(jsonPath("$.alertas[0].tipo").value("danger"));
    }
}
