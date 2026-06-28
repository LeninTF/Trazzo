package trazzo.back.saasglobal.infrastructure.adapters.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import trazzo.back.saasglobal.application.dto.result.TenantResultDto;
import trazzo.back.saasglobal.application.port.in.CreateTrialTenantUseCase;

@WebMvcTest(TenantController.class)
class TenantControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean CreateTrialTenantUseCase createTrialTenantUseCase;

    @Test
    @WithMockUser
    void createTrial_returns201WithBody() throws Exception {
        var result = new TenantResultDto("id-1", "acme", 1, true, null, null);
        when(createTrialTenantUseCase.createTrial(any())).thenReturn(result);

        mockMvc.perform(post("/api/v1/tenants/trial")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "subDomain": "acme",
                                  "planId": 1,
                                  "holdingId": 10,
                                  "dbHost": "localhost",
                                  "dbPort": "5432",
                                  "dbName": "testdb",
                                  "dbUser": "user",
                                  "dbPassword": "pass"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("id-1"))
                .andExpect(jsonPath("$.subDomain").value("acme"))
                .andExpect(jsonPath("$.activated").value(true));
    }

    @Test
    @WithMockUser
    void createTrial_returns400WhenRequiredFieldMissing() throws Exception {
        mockMvc.perform(post("/api/v1/tenants/trial")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "planId": 1,
                                  "holdingId": 10
                                }
                                """))
                .andExpect(status().isUnprocessableEntity());
    }
}
