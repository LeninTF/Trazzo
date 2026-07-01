package trazzo.back.audit.infrastructure.adapters.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import trazzo.back.audit.application.dto.result.PaginatedResult;
import trazzo.back.audit.application.dto.result.SessionResult;
import trazzo.back.audit.application.port.in.SessionUseCase;
import trazzo.back.audit.domain.exception.AuditNotFoundException;
import trazzo.back.audit.domain.model.tenant.SessionState;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SessionsController.class)
@AutoConfigureMockMvc(addFilters = false)
class SessionsControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    SessionUseCase sessionUseCase;

    private static SessionResult aResult() {
        return new SessionResult(1L, "tu-1", "hash123", "192.168.1.1",
                "Mozilla/5.0", "fp-1", LocalDateTime.now(), LocalDateTime.now(),
                null, LocalDateTime.now().plusDays(1), SessionState.ACTIVE,
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void list_shouldReturn200WithSessionListResponse() throws Exception {
        var paginated = new PaginatedResult<SessionResult>(List.of(aResult()), 0, 10, 1, 1);
        when(sessionUseCase.findAll(any(), any(), any(), anyInt(), anyInt(), any()))
                .thenReturn(paginated);

        mockMvc.perform(get("/audit/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].tenant_user_id").value("tu-1"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getById_shouldReturn200WhenFound() throws Exception {
        when(sessionUseCase.findById(1L)).thenReturn(aResult());

        mockMvc.perform(get("/audit/sessions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.state").value("ACTIVE"));
    }

    @Test
    void getById_shouldReturn404WhenNotFound() throws Exception {
        when(sessionUseCase.findById(anyLong())).thenThrow(new AuditNotFoundException("Session not found: 999"));

        mockMvc.perform(get("/audit/sessions/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }
}
