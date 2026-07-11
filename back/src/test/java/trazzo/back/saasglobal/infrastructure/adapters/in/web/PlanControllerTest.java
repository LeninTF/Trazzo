package trazzo.back.saasglobal.infrastructure.adapters.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import trazzo.back.saasglobal.application.dto.result.PlanResult;
import trazzo.back.saasglobal.application.port.in.PlanUseCase;
import trazzo.back.saasglobal.domain.exception.InvalidSubscriptionTransitionException;
import trazzo.back.saasglobal.domain.exception.TenantAlreadyActivatedException;
import trazzo.back.saasglobal.domain.exception.TenantValidationException;
import trazzo.back.saasglobal.domain.exception.UserValidationException;

@WebMvcTest(PlanController.class)
class PlanControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean PlanUseCase planUseCase;

    private static PlanResult result(boolean active) {
        return new PlanResult(1, "Basic", BigDecimal.valueOf(99), BigDecimal.valueOf(999), "SOLES", "MONTHLY",
                active, LocalDateTime.now(), java.util.Map.of());
    }

    @Test
    @WithMockUser
    void listAll_returns200WithArray() throws Exception {
        when(planUseCase.listAll()).thenReturn(List.of(result(true)));

        mockMvc.perform(get("/saas/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Basic"));
    }

    @Test
    @WithMockUser
    void listActive_returns200WithArray() throws Exception {
        when(planUseCase.listActive()).thenReturn(List.of(result(true)));

        mockMvc.perform(get("/saas/plans/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    @WithMockUser
    void create_returns201WithBody() throws Exception {
        when(planUseCase.create(any())).thenReturn(result(true));

        mockMvc.perform(post("/saas/plans")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Basic","price":99,"currency":"SOLES","billingPeriod":"MONTHLY"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Basic"));
    }

    @Test
    @WithMockUser
    void create_returns409WhenPlanNameAlreadyExists() throws Exception {
        when(planUseCase.create(any())).thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        mockMvc.perform(post("/saas/plans")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Basic","price":99,"currency":"SOLES","billingPeriod":"MONTHLY"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @WithMockUser
    void create_returns400WhenTenantValidationExceptionThrown() throws Exception {
        when(planUseCase.create(any())).thenThrow(new TenantValidationException("Invalid tenant"));

        mockMvc.perform(post("/saas/plans")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Basic","price":99,"currency":"SOLES","billingPeriod":"MONTHLY"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Error"));
    }

    @Test
    @WithMockUser
    void create_returns400WhenUserValidationExceptionThrown() throws Exception {
        when(planUseCase.create(any())).thenThrow(new UserValidationException("Invalid user"));

        mockMvc.perform(post("/saas/plans")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Basic","price":99,"currency":"SOLES","billingPeriod":"MONTHLY"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Error"));
    }

    @Test
    @WithMockUser
    void create_returns409WhenTenantAlreadyActivatedExceptionThrown() throws Exception {
        when(planUseCase.create(any())).thenThrow(new TenantAlreadyActivatedException("Already activated"));

        mockMvc.perform(post("/saas/plans")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Basic","price":99,"currency":"SOLES","billingPeriod":"MONTHLY"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser
    void create_returns409WhenInvalidSubscriptionTransitionExceptionThrown() throws Exception {
        when(planUseCase.create(any())).thenThrow(new InvalidSubscriptionTransitionException("Bad transition"));

        mockMvc.perform(post("/saas/plans")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Basic","price":99,"currency":"SOLES","billingPeriod":"MONTHLY"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser
    void create_returns500WithCleanBodyForUnexpectedException() throws Exception {
        when(planUseCase.create(any())).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(post("/saas/plans")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Basic","price":99,"currency":"SOLES","billingPeriod":"MONTHLY"}
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal Server Error"));
    }

    @Test
    @WithMockUser
    void create_returns400WhenRequiredFieldMissing() throws Exception {
        mockMvc.perform(post("/saas/plans")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"price":99,"currency":"SOLES"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void getById_returns200() throws Exception {
        when(planUseCase.getById(1)).thenReturn(result(true));

        mockMvc.perform(get("/saas/plans/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser
    void update_returns200WithBody() throws Exception {
        when(planUseCase.update(any())).thenReturn(result(true));

        mockMvc.perform(put("/saas/plans/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Basic","price":99,"currency":"SOLES","billingPeriod":"MONTHLY"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser
    void update_returns400WhenRequiredFieldMissing() throws Exception {
        mockMvc.perform(put("/saas/plans/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"price":99,"currency":"SOLES"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void activate_returns200() throws Exception {
        when(planUseCase.activate(1)).thenReturn(result(true));

        mockMvc.perform(put("/saas/plans/1/activate").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @WithMockUser
    void deactivate_returns200() throws Exception {
        when(planUseCase.deactivate(1)).thenReturn(result(false));

        mockMvc.perform(put("/saas/plans/1/deactivate").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @WithMockUser
    void delete_returns204() throws Exception {
        doNothing().when(planUseCase).deleteById(1);

        mockMvc.perform(delete("/saas/plans/1").with(csrf()))
                .andExpect(status().isNoContent());
    }
}
