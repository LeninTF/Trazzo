package trazzo.back.saasglobal.infrastructure.adapters.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import trazzo.back.saasglobal.application.dto.result.PaginatedResult;
import trazzo.back.saasglobal.application.dto.result.TenantMetricsResult;
import trazzo.back.saasglobal.application.dto.result.TenantResult;
import trazzo.back.saasglobal.application.port.in.SaasTenantUseCase;
import trazzo.back.shared.security.AuthenticatedUser;

@WebMvcTest(SaasTenantController.class)
class SaasTenantControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean SaasTenantUseCase tenantUseCase;

    private static UsernamePasswordAuthenticationToken adminAuth() {
        var authUser = new AuthenticatedUser(UUID.randomUUID(), "admin@trazzo.pe", "pass", List.of(), true);
        return new UsernamePasswordAuthenticationToken(authUser, null,
                List.of(new SimpleGrantedAuthority("ROLE_SAAS_ADMIN")));
    }

    private static TenantResult result(String status) {
        return new TenantResult("tenant-1", "acme", 10, "Acme SAC", 1, "Plan Demo",
                status, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void listAll_returns200() throws Exception {
        when(tenantUseCase.listAll(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(PaginatedResult.of(List.of(result("ACTIVE")), 0, 20, 1));

        mockMvc.perform(get("/saas/tenants").with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("tenant-1"))
                .andExpect(jsonPath("$.content[0].holdingName").value("Acme SAC"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getMetrics_returns200() throws Exception {
        when(tenantUseCase.getMetrics()).thenReturn(
                new TenantMetricsResult(10, 5.0, 8, 80.0, 2, 30, 0.0, 0.0));

        mockMvc.perform(get("/saas/tenants/metrics").with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(10))
                .andExpect(jsonPath("$.nuevosMeta").value(30));
    }

    @Test
    void getById_returns200() throws Exception {
        when(tenantUseCase.getById("tenant-1")).thenReturn(result("TRIAL"));

        mockMvc.perform(get("/saas/tenants/tenant-1").with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TRIAL"));
    }

    @Test
    void suspend_returns200() throws Exception {
        when(tenantUseCase.suspend("tenant-1")).thenReturn(result("SUSPENDED"));

        mockMvc.perform(put("/saas/tenants/tenant-1/suspend").with(authentication(adminAuth())).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));
    }

    @Test
    void reactivate_returns200() throws Exception {
        when(tenantUseCase.reactivate("tenant-1")).thenReturn(result("ACTIVE"));

        mockMvc.perform(put("/saas/tenants/tenant-1/reactivate").with(authentication(adminAuth())).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void updateBranding_returns200() throws Exception {
        when(tenantUseCase.updateBranding(eq("tenant-1"), any(), any(), any(), any())).thenReturn(result("ACTIVE"));

        mockMvc.perform(put("/saas/tenants/tenant-1/branding")
                        .with(authentication(adminAuth()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"logoUrl":"http://logo","slogan":"s","primaryColor":"#111","secondaryColor":"#222"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/saas/tenants/tenant-1").with(authentication(adminAuth())).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void listAll_returns401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/saas/tenants"))
                .andExpect(status().isUnauthorized());
    }
}
