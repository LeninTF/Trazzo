package trazzo.back.audit.infrastructure.adapters.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import trazzo.back.audit.application.dto.result.LogInHistoryResult;
import trazzo.back.audit.application.dto.result.PaginatedResult;
import trazzo.back.audit.application.port.in.LoginHistoryUseCase;
import trazzo.back.audit.domain.exception.AuditNotFoundException;
import trazzo.back.audit.domain.model.master.StatusLogin;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LoginHistoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class LoginHistoryControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    LoginHistoryUseCase loginHistoryUseCase;

    private static LogInHistoryResult aResult() {
        return new LogInHistoryResult(1L, "u-1", "user@test.com",
                StatusLogin.SUCCES, "192.168.1.1", "Mozilla/5.0", LocalDateTime.now());
    }

    @Test
    void list_shouldReturn200WithLogInHistoryListResponse() throws Exception {
        var paginated = new PaginatedResult<LogInHistoryResult>(List.of(aResult()), 0, 10, 1, 1);
        when(loginHistoryUseCase.findAll(any(), any(), any(), any(), any(), anyInt(), anyInt(), any()))
                .thenReturn(paginated);

        mockMvc.perform(get("/audit/login-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].user_id").value("u-1"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getById_shouldReturn200WhenFound() throws Exception {
        when(loginHistoryUseCase.findById(1L)).thenReturn(aResult());

        mockMvc.perform(get("/audit/login-history/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("SUCCES"));
    }

    @Test
    void getById_shouldReturn404WhenNotFound() throws Exception {
        when(loginHistoryUseCase.findById(anyLong())).thenThrow(new AuditNotFoundException("Login history not found: 999"));

        mockMvc.perform(get("/audit/login-history/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }
}
