package trazzo.back.saasglobal.infrastructure.adapters.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import trazzo.back.saasglobal.application.dto.result.HoldingResult;
import trazzo.back.saasglobal.application.port.in.HoldingUseCase;

@WebMvcTest(HoldingController.class)
class HoldingControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean HoldingUseCase holdingUseCase;

    private static HoldingResult result(boolean active) {
        var now = LocalDateTime.now();
        return new HoldingResult(1, "20111111111", "Corp SA", "PUBLICO", active, now, now);
    }

    @Test
    @WithMockUser
    void listAll_returns200WithArray() throws Exception {
        when(holdingUseCase.listAll()).thenReturn(List.of(result(true)));

        mockMvc.perform(get("/api/v1/saas/holdings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].legalName").value("Corp SA"));
    }

    @Test
    @WithMockUser
    void create_returns201WithBody() throws Exception {
        when(holdingUseCase.create(any())).thenReturn(result(true));

        mockMvc.perform(post("/api/v1/saas/holdings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"taxId":"20111111111","legalName":"Corp SA","type":"PUBLICO"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.taxId").value("20111111111"));
    }

    @Test
    @WithMockUser
    void create_returns400WhenRequiredFieldMissing() throws Exception {
        mockMvc.perform(post("/api/v1/saas/holdings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"legalName":"Corp SA","type":"PUBLICO"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void getById_returns200() throws Exception {
        when(holdingUseCase.getById(1)).thenReturn(result(true));

        mockMvc.perform(get("/api/v1/saas/holdings/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser
    void update_returns200WithBody() throws Exception {
        when(holdingUseCase.update(any())).thenReturn(result(true));

        mockMvc.perform(put("/api/v1/saas/holdings/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"legalName":"Corp SA","type":"PUBLICO"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser
    void update_returns400WhenRequiredFieldMissing() throws Exception {
        mockMvc.perform(put("/api/v1/saas/holdings/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"PUBLICO"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void activate_returns200() throws Exception {
        when(holdingUseCase.activate(1)).thenReturn(result(true));

        mockMvc.perform(put("/api/v1/saas/holdings/1/activate").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @WithMockUser
    void deactivate_returns200() throws Exception {
        when(holdingUseCase.deactivate(1)).thenReturn(result(false));

        mockMvc.perform(put("/api/v1/saas/holdings/1/deactivate").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @WithMockUser
    void delete_returns204() throws Exception {
        doNothing().when(holdingUseCase).deleteById(1);

        mockMvc.perform(delete("/api/v1/saas/holdings/1").with(csrf()))
                .andExpect(status().isNoContent());
    }
}
