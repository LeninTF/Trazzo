package trazzo.back.saasglobal.infrastructure.adapters.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import trazzo.back.saasglobal.application.dto.result.PaginatedResult;
import trazzo.back.saasglobal.application.dto.result.RequestCommentResult;
import trazzo.back.saasglobal.application.dto.result.RequestContactResult;
import trazzo.back.saasglobal.application.dto.result.RequestDetailResult;
import trazzo.back.saasglobal.application.dto.result.RequestResult;
import trazzo.back.saasglobal.application.port.in.RequestUseCase;
import trazzo.back.shared.security.AuthenticatedUser;

@WebMvcTest(SaasRequestController.class)
class SaasRequestControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean RequestUseCase requestUseCase;

    private static UsernamePasswordAuthenticationToken adminAuth() {
        var authUser = new AuthenticatedUser(UUID.randomUUID(), "admin@trazzo.pe", "pass", List.of(), true);
        return new UsernamePasswordAuthenticationToken(authUser, null, List.of());
    }

    private static RequestContactResult contact() {
        return new RequestContactResult("Ana", "Perez", "ana@example.com", "999999999", "20123456789", "Acme SAC");
    }

    @Test
    void list_returns200WithPaginatedResults() throws Exception {
        var now = LocalDateTime.now();
        var result = new RequestResult(1, "TRIAL", "title", "message", "PENDING", now, now, contact());
        when(requestUseCase.listAll(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(PaginatedResult.of(List.of(result), 0, 20, 1));

        mockMvc.perform(get("/saas/requests").with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getById_returns200WithDetail() throws Exception {
        var now = LocalDateTime.now();
        when(requestUseCase.getById(1)).thenReturn(new RequestDetailResult(
                1, "TRIAL", "title", "message", "PENDING", now, now, contact(), List.of(), List.of()));

        mockMvc.perform(get("/saas/requests/1").with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.contact.email").value("ana@example.com"));
    }

    @Test
    void changeStatus_returns200WithUpdatedResult() throws Exception {
        var now = LocalDateTime.now();
        when(requestUseCase.changeStatus(any())).thenReturn(
                new RequestResult(1, "TRIAL", "title", "message", "APPROVED", now, now, contact()));

        mockMvc.perform(patch("/saas/requests/1/status")
                        .with(authentication(adminAuth()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"APPROVED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void changeStatus_returns400WhenStatusMissing() throws Exception {
        mockMvc.perform(patch("/saas/requests/1/status")
                        .with(authentication(adminAuth()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addComment_returns201WithComment() throws Exception {
        when(requestUseCase.addComment(any())).thenReturn(
                new RequestCommentResult(5, "Todo en orden", "admin-1", LocalDateTime.now()));

        mockMvc.perform(post("/saas/requests/1/comments")
                        .with(authentication(adminAuth()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"comment":"Todo en orden"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.comment").value("Todo en orden"));
    }
}
