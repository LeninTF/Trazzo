package trazzo.back.audit.infrastructure.adapters.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import trazzo.back.audit.application.dto.result.PaginatedResult;
import trazzo.back.audit.application.dto.result.SystemAuditResult;
import trazzo.back.audit.application.port.in.SystemAuditUseCase;
import trazzo.back.audit.domain.exception.AuditNotFoundException;
import trazzo.back.audit.domain.model.tenant.HttpMethod;
import trazzo.back.audit.domain.model.tenant.SystemActions;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SystemAuditController.class)
@AutoConfigureMockMvc(addFilters = false)
class SystemAuditControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    SystemAuditUseCase systemAuditUseCase;

    private static SystemAuditResult aResult() {
        return new SystemAuditResult(1L, "utu-1", SystemActions.CREATE, "Module",
                "Entity", "e-1", HttpMethod.POST, "/api/test", "description",
                Map.of("old", "value"), Map.of("new", "value"),
                "192.168.1.1", "SUCCESS", LocalDateTime.now());
    }

    @Test
    void list_shouldReturn200WithSystemAuditListResponse() throws Exception {
        var paginated = new PaginatedResult<SystemAuditResult>(List.of(aResult()), 0, 10, 1, 1);
        when(systemAuditUseCase.findAll(any(), any(), any(), any(), any(), anyInt(), anyInt(), any()))
                .thenReturn(paginated);

        mockMvc.perform(get("/audit/system-audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].module").value("Module"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getById_shouldReturn200WhenFound() throws Exception {
        when(systemAuditUseCase.findById(1L)).thenReturn(aResult());

        mockMvc.perform(get("/audit/system-audit/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.system_actions").value("CREATE"));
    }

    @Test
    void getById_shouldReturn404WhenNotFound() throws Exception {
        when(systemAuditUseCase.findById(anyLong())).thenThrow(new AuditNotFoundException("System audit not found: 999"));

        mockMvc.perform(get("/audit/system-audit/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }
}
