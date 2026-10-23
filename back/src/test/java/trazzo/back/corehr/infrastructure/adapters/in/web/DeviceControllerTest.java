package trazzo.back.corehr.infrastructure.adapters.in.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import trazzo.back.shared.security.TenantSecuritySlice;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import trazzo.back.corehr.application.dto.command.CreateDeviceCommand;
import trazzo.back.corehr.application.dto.command.PatchDeviceCommand;
import trazzo.back.corehr.application.dto.result.DeviceResult;
import trazzo.back.corehr.application.dto.result.IssuedAgentToken;
import trazzo.back.corehr.application.dto.result.PaginatedResult;
import trazzo.back.corehr.application.port.in.DeviceUseCase;
import trazzo.back.corehr.application.usecase.DeviceAgentTokenService;
import trazzo.back.corehr.domain.exception.InactiveDeviceException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import trazzo.back.shared.tenancy.TenantContext;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DeviceController.class)
@AutoConfigureMockMvc(addFilters = false)
@TenantSecuritySlice
class DeviceControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    DeviceUseCase deviceUseCase;

    @MockitoBean
    DeviceAgentTokenService deviceAgentTokenService;

    @AfterEach
    void clearContexts() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    private static void actingAsTenantUserWith(String... authorities) {
        TenantContext.set("tenant_demo");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin@demo", null,
                java.util.Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList()));
    }

    @Test
    void issueAgentToken_returnsTheTokenOnlyInThisResponse() throws Exception {
        actingAsTenantUserWith("configuracion-tenant.gestionar-metodos");
        UUID tenant = UUID.fromString("11111111-2222-3333-4444-555555555555");
        when(deviceAgentTokenService.issue("tenant_demo", 7L))
                .thenReturn(new IssuedAgentToken("trzd_secreto", tenant, "DVC-007", LocalDateTime.of(2026, 10, 23, 9, 0)));

        mockMvc.perform(post("/corehr/devices/7/agent-token"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("trzd_secreto"))
                .andExpect(jsonPath("$.tenant_id").value(tenant.toString()))
                .andExpect(jsonPath("$.device_code").value("DVC-007"))
                .andExpect(jsonPath("$.created_at").exists());
    }

    @Test
    void issueAgentToken_requiresTheDeviceManagementPermission() throws Exception {
        actingAsTenantUserWith("asistencia-docente.ver-historial");

        mockMvc.perform(post("/corehr/devices/7/agent-token"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(deviceAgentTokenService);
    }

    @Test
    void revokeAgentToken_returns204() throws Exception {
        actingAsTenantUserWith("configuracion-tenant.gestionar-metodos");

        mockMvc.perform(delete("/corehr/devices/7/agent-token"))
                .andExpect(status().isNoContent());
        verify(deviceAgentTokenService).revoke("tenant_demo", 7L);
    }

    @Test
    void revokeAgentToken_requiresTheDeviceManagementPermission() throws Exception {
        actingAsTenantUserWith("asistencia-docente.ver-historial");

        mockMvc.perform(delete("/corehr/devices/7/agent-token"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(deviceAgentTokenService);
    }

    @Test
    void deletingADeviceRevokesItsTokensFirst() throws Exception {
        TenantContext.set("tenant_demo");

        mockMvc.perform(delete("/corehr/devices/7"))
                .andExpect(status().isNoContent());

        var order = inOrder(deviceAgentTokenService, deviceUseCase);
        order.verify(deviceAgentTokenService).revoke("tenant_demo", 7L);
        order.verify(deviceUseCase).deleteById(7L);
    }

    private static final LocalDateTime NOW = LocalDateTime.now();

    private static DeviceResult aResult() {
        return new DeviceResult(1L, "D-001", "Device1", 10L, "Branch1",
                "192.168.1.1", 8080, "Office", true, NOW);
    }

    @Test
    void list_shouldReturn200WithDeviceListResponse() throws Exception {
        var paginated = new PaginatedResult<DeviceResult>(List.of(aResult()), 0, 20, 1, 1);
        when(deviceUseCase.findAll(any(), any(), anyInt(), anyInt())).thenReturn(paginated);

        mockMvc.perform(get("/corehr/devices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("D-001"))
                .andExpect(jsonPath("$.content[0].state").value(true))
                .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    void create_shouldReturn201() throws Exception {
        when(deviceUseCase.create(any(CreateDeviceCommand.class))).thenReturn(aResult());

        mockMvc.perform(post("/corehr/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "D-001", "name": "Device1", "branch_id": 10}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("D-001"));
    }

    @Test
    void create_shouldReturn400WhenCodeIsBlank() throws Exception {
        mockMvc.perform(post("/corehr/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "", "branch_id": 10}
                                """))
                .andExpect(status().isBadRequest());
    }

    // El código viaja a URLs del agente y al comando de Configure-Agent.ps1 que el panel genera.
    @Test
    void create_shouldReturn400WhenCodeHasCharactersOutsideTheAllowlist() throws Exception {
        mockMvc.perform(post("/corehr/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "ZK\\"; $(Remove-Item C:\\\\ -Recurse) #", "branch_id": 10}
                                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(deviceUseCase);
    }

    @Test
    void create_shouldReturn400WhenBranchIdIsMissing() throws Exception {
        mockMvc.perform(post("/corehr/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "D-001"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_shouldReturn422WhenUseCaseThrowsInactiveDeviceException() throws Exception {
        when(deviceUseCase.create(any(CreateDeviceCommand.class)))
                .thenThrow(new InactiveDeviceException("Device is inactive"));

        mockMvc.perform(post("/corehr/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "D-002", "branch_id": 10}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Unprocessable Entity"))
                .andExpect(jsonPath("$.message").value("Device is inactive"));
    }

    @Test
    void getById_shouldReturn200WhenFound() throws Exception {
        when(deviceUseCase.findById(1L)).thenReturn(Optional.of(aResult()));

        mockMvc.perform(get("/corehr/devices/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getById_shouldReturn404WhenNotFound() throws Exception {
        when(deviceUseCase.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/corehr/devices/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void patch_shouldReturn200() throws Exception {
        when(deviceUseCase.patch(anyLong(), any(PatchDeviceCommand.class))).thenReturn(aResult());

        mockMvc.perform(patch("/corehr/devices/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Updated Device"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Device1"));
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        doNothing().when(deviceUseCase).deleteById(1L);

        mockMvc.perform(delete("/corehr/devices/1"))
                .andExpect(status().isNoContent());
    }
}
