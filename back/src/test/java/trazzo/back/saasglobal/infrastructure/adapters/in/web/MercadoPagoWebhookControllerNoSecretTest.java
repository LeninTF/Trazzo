package trazzo.back.saasglobal.infrastructure.adapters.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import trazzo.back.saasglobal.application.port.in.ProcessMercadoPagoWebhookUseCase;
import trazzo.back.shared.security.SecurityConfig;

/**
 * mercadopago.webhook-secret is unset in src/test/resources/application.properties (matches
 * a real environment before the MP dashboard notification URL is configured) — this must
 * never fall back to processing unverified notifications.
 */
@WebMvcTest(MercadoPagoWebhookController.class)
@Import(SecurityConfig.class)
class MercadoPagoWebhookControllerNoSecretTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean ProcessMercadoPagoWebhookUseCase webhookUseCase;

    @Test
    void receive_returns401_whenWebhookSecretNotConfigured() throws Exception {
        mockMvc.perform(post("/webhooks/mercadopago")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("x-signature", "ts=1700000000000,v1=deadbeef")
                        .header("x-request-id", "req-1")
                        .content("""
                                {"id":"evt-1","type":"subscription_preapproval","action":"created","data":{"id":"pre-1"}}
                                """))
                .andExpect(status().isUnauthorized());

        verify(webhookUseCase, never()).process(any());
    }
}
