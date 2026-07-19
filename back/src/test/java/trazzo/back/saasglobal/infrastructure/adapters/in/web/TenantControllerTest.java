package trazzo.back.saasglobal.infrastructure.adapters.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import trazzo.back.saasglobal.application.dto.result.TenantResultDto;
import trazzo.back.saasglobal.application.port.in.CreateTrialTenantUseCase;

@WebMvcTest(TenantController.class)
@AutoConfigureMockMvc(addFilters = false)
class TenantControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean CreateTrialTenantUseCase createTrialTenantUseCase;

    @Test
    void createTrial_returns201WithBody() throws Exception {
        var result = new TenantResultDto("id-1", "acme", 1, true, null, null);
        when(createTrialTenantUseCase.createTrial(any())).thenReturn(result);

        mockMvc.perform(post("/tenants/trial")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subDomain":"acme","planId":1,"holdingId":10}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("id-1"))
                .andExpect(jsonPath("$.subDomain").value("acme"))
                .andExpect(jsonPath("$.activated").value(true));
    }

    @Test
    void createTrial_returns400WhenRequiredFieldMissing() throws Exception {
        mockMvc.perform(post("/tenants/trial")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"planId":1}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTrial_worksWithoutAuthentication() throws Exception {
        var result = new TenantResultDto("id-3", "acme", 1, true, null, null);
        when(createTrialTenantUseCase.createTrial(any())).thenReturn(result);

        mockMvc.perform(post("/tenants/trial")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subDomain":"acme","planId":1,"holdingId":10}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void createTrial_withAllOptionalFields() throws Exception {
        var result = new TenantResultDto("id-2", "beach", 2, true, null, null);
        when(createTrialTenantUseCase.createTrial(any())).thenReturn(result);

        mockMvc.perform(post("/tenants/trial")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subDomain":"beach","planId":2,"holdingId":20,
                                 "logoUrl":"http://logo.png","slogan":"Best",
                                 "primaryColor":"#FF0000","secondaryColor":"#00FF00"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subDomain").value("beach"));
    }
}
