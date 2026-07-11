package trazzo.back.saasglobal.infrastructure.adapters.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import trazzo.back.saasglobal.application.dto.result.InvoiceResult;
import trazzo.back.saasglobal.application.dto.result.PaginatedResult;
import trazzo.back.saasglobal.application.dto.result.PlanResult;
import trazzo.back.saasglobal.application.port.in.InvoiceUseCase;
import trazzo.back.saasglobal.application.port.in.PlanUseCase;
import trazzo.back.saasglobal.application.port.out.TenantRepositoryPort;
import trazzo.back.saasglobal.application.port.out.UserRepositoryPort;
import trazzo.back.saasglobal.domain.model.iam.User;
import trazzo.back.saasglobal.domain.model.multitenancy.Tenant;
import trazzo.back.shared.security.AuthenticatedUser;
import trazzo.back.shared.security.SecurityConfig;

@WebMvcTest(TenantBillingController.class)
@Import(SecurityConfig.class)
class TenantBillingControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Autowired MockMvc mockMvc;
    @MockitoBean PlanUseCase planUseCase;
    @MockitoBean InvoiceUseCase invoiceUseCase;
    @MockitoBean TenantRepositoryPort tenantRepository;
    @MockitoBean UserRepositoryPort userRepository;

    private static UsernamePasswordAuthenticationToken authenticatedTenantUser() {
        var authUser = new AuthenticatedUser(USER_ID, "tenant.admin@demo.pe", "pass", List.of(), true);
        return new UsernamePasswordAuthenticationToken(authUser, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    private static User tenantScopedUser() {
        return User.create(1, "tenant-1", "tenant.admin@demo.pe", null, "encoded");
    }

    private static PlanResult planResult() {
        return new PlanResult(2, "Plan Demo", BigDecimal.ZERO, null, "SOLES", "MONTHLY", true,
                LocalDateTime.now(), Map.of());
    }

    @Test
    void getCurrentPlan_returns200_withTenantsPlan() throws Exception {
        when(userRepository.findById(USER_ID.toString())).thenReturn(Optional.of(tenantScopedUser()));
        when(tenantRepository.findById("tenant-1")).thenReturn(Optional.of(
                Tenant.restore("tenant-1", null, "demo", 2, null, null, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), null)));
        when(planUseCase.getById(2)).thenReturn(planResult());

        mockMvc.perform(get("/org/billing/plan").with(authentication(authenticatedTenantUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Plan Demo"));
    }

    @Test
    void getCurrentPlan_returns401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/org/billing/plan")).andExpect(status().isUnauthorized());
    }

    @Test
    void listAvailablePlans_returns200() throws Exception {
        when(planUseCase.listActive()).thenReturn(List.of(planResult()));

        mockMvc.perform(get("/org/billing/plans").with(authentication(authenticatedTenantUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Plan Demo"));
    }

    @Test
    void listMyInvoices_scopesToCallersTenant() throws Exception {
        when(userRepository.findById(USER_ID.toString())).thenReturn(Optional.of(tenantScopedUser()));
        when(invoiceUseCase.listAll(any(), eq("tenant-1"), any(), any(), anyInt(), anyInt()))
                .thenReturn(PaginatedResult.of(List.of(invoiceResult()), 0, 20, 1));

        mockMvc.perform(get("/org/billing/invoices").with(authentication(authenticatedTenantUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("inv-1"));
    }

    private static InvoiceResult invoiceResult() {
        return new InvoiceResult("inv-1", "tenant-1", "F001", "001", "01_FACTURA",
                "20222222222", "Cliente SAC", BigDecimal.TEN, BigDecimal.ONE, BigDecimal.TEN,
                "PENDIENTE", null, LocalDateTime.now());
    }
}
