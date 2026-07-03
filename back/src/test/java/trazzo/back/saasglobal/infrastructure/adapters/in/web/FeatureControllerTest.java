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
import trazzo.back.saasglobal.application.dto.result.FeatureResult;
import trazzo.back.saasglobal.application.port.in.FeatureUseCase;

@WebMvcTest(FeatureController.class)
class FeatureControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean FeatureUseCase featureUseCase;

    private static FeatureResult result() {
        var now = LocalDateTime.now();
        return new FeatureResult(1, "Biometric", "Fingerprint auth", now, now);
    }

    @Test
    @WithMockUser
    void listAll_returns200WithArray() throws Exception {
        when(featureUseCase.listAll()).thenReturn(List.of(result()));

        mockMvc.perform(get("/saas/features"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Biometric"));
    }

    @Test
    @WithMockUser
    void create_returns201WithBody() throws Exception {
        when(featureUseCase.create(any())).thenReturn(result());

        mockMvc.perform(post("/saas/features")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Biometric","description":"Fingerprint auth"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Biometric"));
    }

    @Test
    @WithMockUser
    void create_returns400WhenNameMissing() throws Exception {
        mockMvc.perform(post("/saas/features")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Fingerprint auth"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void getById_returns200() throws Exception {
        when(featureUseCase.getById(1)).thenReturn(result());

        mockMvc.perform(get("/saas/features/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser
    void update_returns200WithBody() throws Exception {
        when(featureUseCase.update(any())).thenReturn(result());

        mockMvc.perform(put("/saas/features/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Biometric","description":"Fingerprint auth"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser
    void update_returns400WhenNameMissing() throws Exception {
        mockMvc.perform(put("/saas/features/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Fingerprint auth"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void deleteById_returns204() throws Exception {
        doNothing().when(featureUseCase).deleteById(1);

        mockMvc.perform(delete("/saas/features/1").with(csrf()))
                .andExpect(status().isNoContent());
    }
}
