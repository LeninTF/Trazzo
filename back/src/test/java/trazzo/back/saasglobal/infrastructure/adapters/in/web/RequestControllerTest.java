package trazzo.back.saasglobal.infrastructure.adapters.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import trazzo.back.saasglobal.application.dto.result.RequestContactResult;
import trazzo.back.saasglobal.application.dto.result.RequestResult;
import trazzo.back.saasglobal.application.port.in.RequestUseCase;
import trazzo.back.saasglobal.domain.exception.RequestRateLimitException;
import trazzo.back.shared.security.SecurityConfig;

@WebMvcTest(RequestController.class)
@Import(SecurityConfig.class)
class RequestControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean RequestUseCase requestUseCase;

    private static final String VALID_BODY = """
            {"type":"trial","name":"Ana","lastName":"Perez","email":"ana@example.com",
             "phoneNumber":"999999999","taxId":"20123456789","companyName":"Acme SAC",
             "message":"Quiero una demo"}
            """;

    @Test
    void submit_withoutAuthentication_returns201() throws Exception {
        var now = LocalDateTime.now();
        var contact = new RequestContactResult("Ana", "Perez", "ana@example.com", "999999999", "20123456789", "Acme SAC");
        when(requestUseCase.submit(any())).thenReturn(
                new RequestResult(1, "TRIAL", "Solicitud de trial - Acme SAC", "Quiero una demo", "PENDING", now, now, contact));

        mockMvc.perform(post("/requests")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void submit_returns429WhenRateLimited() throws Exception {
        when(requestUseCase.submit(any())).thenThrow(new RequestRateLimitException("Too many requests"));

        mockMvc.perform(post("/requests")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void submit_returns400WhenRequiredFieldMissing() throws Exception {
        mockMvc.perform(post("/requests")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"trial","name":"Ana"}
                                """))
                .andExpect(status().isBadRequest());
    }
}
