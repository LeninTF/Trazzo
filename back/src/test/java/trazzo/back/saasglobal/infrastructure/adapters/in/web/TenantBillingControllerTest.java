package trazzo.back.saasglobal.infrastructure.adapters.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import trazzo.back.saasglobal.application.dto.command.SubscribeToPlanCommand;
import trazzo.back.saasglobal.application.dto.result.InvoiceResult;
import trazzo.back.saasglobal.application.dto.result.PaginatedResult;
import trazzo.back.saasglobal.application.dto.result.PlanResult;
import trazzo.back.saasglobal.application.dto.result.SubscribeToPlanResult;
import trazzo.back.saasglobal.application.port.in.InvoiceUseCase;
import trazzo.back.saasglobal.application.port.in.PlanUseCase;
import trazzo.back.saasglobal.application.port.in.SubscribeToPlanUseCase;
import trazzo.back.saasglobal.application.port.out.TenantRepositoryPort;
import trazzo.back.saasglobal.application.port.out.UserRepositoryPort;
import trazzo.back.saasglobal.domain.model.iam.User;
import trazzo.back.saasglobal.domain.model.multitenancy.Tenant;
import trazzo.back.shared.security.AuthenticatedUser;
import trazzo.back.shared.security.SecurityConfig;
import trazzo.back.shared.security.TenantAuthorization;
import trazzo.back.shared.security.TenantUserIdResolver;

@WebMvcTest(TenantBillingController.class)
@Import({SecurityConfig.class, TenantAuthorization.class})
class TenantBillingControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Autowired MockMvc mockMvc;
    @MockitoBean PlanUseCase planUseCase;
    @MockitoBean InvoiceUseCase invoiceUseCase;
    @MockitoBean TenantRepositoryPort tenantRepository;
    @MockitoBean UserRepositoryPort userRepository;
    @MockitoBean SubscribeToPlanUseCase subscribeToPlanUseCase;
    @MockitoBean TenantUserIdResolver tenantUserIdResolver;

    private static UsernamePasswordAuthenticationToken authenticatedTenantAdmin() {
        return authenticatedTenantUserWith("administrador_tenant");
    }

    private static UsernamePasswordAuthenticationToken authenticatedTenantUserWith(String... permissions) {
        var authorities = new java.util.ArrayList<>(List.of(new SimpleGrantedAuthority("ROLE_USER")));
        java.util.Arrays.stream(permissions).map(SimpleGrantedAuthority::new).forEach(authorities::add);
        var authUser = new AuthenticatedUser(USER_ID, "tenant.admin@demo.pe", "pass", authorities, true);
        return new UsernamePasswordAuthenticationToken(authUser, null, authorities);
    }

    @Test
    void billingIsOnlyForTheTenantAdministrator() throws Exception {
        var director = authenticatedTenantUserWith("gestion-trabajadores.crear", "reportes.exportar-pdf",
                "permisos-licencias.aprobar-incidencias", "configuracion-tenant.gestionar-metodos");

        mockMvc.perform(get("/org/billing/plan").with(authentication(director))).andExpect(status().isForbidden());
        mockMvc.perform(get("/org/billing/plans").with(authentication(director))).andExpect(status().isForbidden());
        mockMvc.perform(get("/org/billing/invoices").with(authentication(director))).andExpect(status().isForbidden());
        mockMvc.perform(post("/org/billing/subscribe")
                        .with(authentication(director))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planId\":3}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(planUseCase, invoiceUseCase, subscribeToPlanUseCase, userRepository, tenantRepository);
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
                Tenant.restore("tenant-1", null, "demo", 2, null, null, LocalDateTime.now(), null, LocalDateTime.now(), LocalDateTime.now(), null)));
        when(planUseCase.getById(2)).thenReturn(planResult());

        mockMvc.perform(get("/org/billing/plan").with(authentication(authenticatedTenantAdmin())))
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

        mockMvc.perform(get("/org/billing/plans").with(authentication(authenticatedTenantAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Plan Demo"));
    }

    @Test
    void listMyInvoices_scopesToCallersTenant() throws Exception {
        when(userRepository.findById(USER_ID.toString())).thenReturn(Optional.of(tenantScopedUser()));
        when(invoiceUseCase.listAll(any(), eq("tenant-1"), any(), any(), anyInt(), anyInt()))
                .thenReturn(PaginatedResult.of(List.of(invoiceResult()), 0, 20, 1));

        mockMvc.perform(get("/org/billing/invoices").with(authentication(authenticatedTenantAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("inv-1"));
    }

    private static InvoiceResult invoiceResult() {
        return new InvoiceResult("inv-1", "tenant-1", "F001", "001", "01_FACTURA",
                "20222222222", "Cliente SAC", BigDecimal.TEN, BigDecimal.ONE, BigDecimal.TEN,
                "PENDIENTE", null, LocalDateTime.now());
    }

    @Test
    void subscribe_returns200_withInitPoint() throws Exception {
        when(userRepository.findById(USER_ID.toString())).thenReturn(Optional.of(tenantScopedUser()));
        when(subscribeToPlanUseCase.subscribe(new SubscribeToPlanCommand("tenant-1", "tenant.admin@demo.pe", 3)))
                .thenReturn(new SubscribeToPlanResult("sub-1", "https://www.mercadopago.com/init-point"));

        mockMvc.perform(post("/org/billing/subscribe")
                        .with(authentication(authenticatedTenantAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planId\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscriptionId").value("sub-1"))
                .andExpect(jsonPath("$.initPoint").value("https://www.mercadopago.com/init-point"));
    }

    @Test
    void subscribe_returns401_whenUnauthenticated() throws Exception {
        mockMvc.perform(post("/org/billing/subscribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planId\":3}"))
                .andExpect(status().isUnauthorized());
    }
}
