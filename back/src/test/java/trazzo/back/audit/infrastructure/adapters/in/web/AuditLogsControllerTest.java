package trazzo.back.audit.infrastructure.adapters.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import trazzo.back.audit.application.dto.result.AuditLogDetailResult;
import trazzo.back.audit.application.dto.result.AuditLogResult;
import trazzo.back.audit.application.dto.result.PaginatedResult;
import trazzo.back.audit.application.port.in.AuditLogUseCase;
import trazzo.back.audit.domain.exception.AuditNotFoundException;
import trazzo.back.audit.domain.model.master.Action;
import trazzo.back.shared.security.AuthenticatedUser;
import trazzo.back.shared.security.SecurityConfig;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuditLogsController.class)
@Import(SecurityConfig.class)
class AuditLogsControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AuditLogUseCase auditLogUseCase;

    private static AuditLogResult aResult() {
        return new AuditLogResult("1", "evt-1", LocalDateTime.now(), "tenant", "t-1",
                "user", "user@test.com", "CREATE", "INFO", "Entity",
                "e-1", "127.0.0.1", "Mozilla/5.0",
                Map.of("old", "value"), Map.of("new", "value"));
    }

    private static AuditLogDetailResult aDetailResult() {
        return new AuditLogDetailResult("1", "Entity", "e-1", Action.CREATE,
                "u-1", "/api/test", "127.0.0.1", "Mozilla/5.0",
                Map.of("old", "value"), Map.of("new", "value"), LocalDateTime.now());
    }

    private static UsernamePasswordAuthenticationToken authWithAuthorities(String... authorities) {
        var authUser = new AuthenticatedUser(UUID.randomUUID(), "admin@trazzo.pe", "pass", List.of(), true);
        var grants = List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList();
        return new UsernamePasswordAuthenticationToken(authUser, null, grants);
    }

    @Test
    void list_shouldReturn200WithAuditLogListResponse() throws Exception {
        var paginated = new PaginatedResult<AuditLogResult>(List.of(aResult()), 0, 10, 1, 1);
        when(auditLogUseCase.findAll(any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), any()))
                .thenReturn(paginated);

        mockMvc.perform(get("/audit/logs")
                        .with(authentication(authWithAuthorities("ROLE_SAAS_ADMIN", "monitoreo-sistema.logs-sistema"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("1"))
                .andExpect(jsonPath("$.content[0].eventId").value("evt-1"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getById_shouldReturn200WhenFound() throws Exception {
        when(auditLogUseCase.findById("1")).thenReturn(aDetailResult());

        mockMvc.perform(get("/audit/logs/1")
                        .with(authentication(authWithAuthorities("ROLE_SAAS_ADMIN", "monitoreo-sistema.logs-sistema"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.accion").value("CREATE"));
    }

    @Test
    void getById_shouldReturn404WhenNotFound() throws Exception {
        when(auditLogUseCase.findById(anyString())).thenThrow(new AuditNotFoundException("Audit not found: 999"));

        mockMvc.perform(get("/audit/logs/999")
                        .with(authentication(authWithAuthorities("ROLE_SAAS_ADMIN", "monitoreo-sistema.logs-sistema"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void list_returns403_whenUserLacksLogsPermission() throws Exception {
        mockMvc.perform(get("/audit/logs")
                        .with(authentication(authWithAuthorities("ROLE_SAAS_ADMIN"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void list_returns401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/audit/logs"))
                .andExpect(status().isUnauthorized());
    }
}
