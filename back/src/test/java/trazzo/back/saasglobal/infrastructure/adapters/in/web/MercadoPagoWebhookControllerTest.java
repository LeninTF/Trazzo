package trazzo.back.saasglobal.infrastructure.adapters.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import trazzo.back.saasglobal.application.dto.command.MercadoPagoWebhookCommand;
import trazzo.back.saasglobal.application.port.in.ProcessMercadoPagoWebhookUseCase;
import trazzo.back.shared.security.SecurityConfig;

@WebMvcTest(MercadoPagoWebhookController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "mercadopago.webhook-secret=test-secret")
class MercadoPagoWebhookControllerTest {

    private static final String SECRET = "test-secret";
    private static final String TS = "1700000000000";
    private static final String REQUEST_ID = "req-1";
    private static final String DATA_ID = "pre-1";
    private static final String PAYLOAD = """
            {"id":"evt-1","type":"subscription_preapproval","action":"created","data":{"id":"pre-1"}}
            """;

    @Autowired MockMvc mockMvc;
    @MockitoBean ProcessMercadoPagoWebhookUseCase webhookUseCase;

    private static String validSignatureHeader() throws Exception {
        String manifest = "id:" + DATA_ID + ";request-id:" + REQUEST_ID + ";ts:" + TS + ";";
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            hex.append(String.format("%02x", b & 0xff));
        }
        return "ts=" + TS + ",v1=" + hex;
    }

    @Test
    void receive_returns200_withValidSignature() throws Exception {
        mockMvc.perform(post("/webhooks/mercadopago")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("x-signature", validSignatureHeader())
                        .header("x-request-id", REQUEST_ID)
                        .content(PAYLOAD))
                .andExpect(status().isOk());

        verify(webhookUseCase).process(new MercadoPagoWebhookCommand(
                "evt-1", "subscription_preapproval", "created", "pre-1", PAYLOAD));
    }

    @Test
    void receive_returns401_withInvalidSignature() throws Exception {
        mockMvc.perform(post("/webhooks/mercadopago")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("x-signature", "ts=" + TS + ",v1=deadbeef")
                        .header("x-request-id", REQUEST_ID)
                        .content(PAYLOAD))
                .andExpect(status().isUnauthorized());

        verify(webhookUseCase, never()).process(any());
    }

    @Test
    void receive_returns401_withMissingSignatureHeader() throws Exception {
        mockMvc.perform(post("/webhooks/mercadopago")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYLOAD))
                .andExpect(status().isUnauthorized());

        verify(webhookUseCase, never()).process(any());
    }

    @Test
    void receive_returns400_withMalformedJson() throws Exception {
        mockMvc.perform(post("/webhooks/mercadopago")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("x-signature", validSignatureHeader())
                        .header("x-request-id", REQUEST_ID)
                        .content("not-json"))
                .andExpect(status().isBadRequest());

        verify(webhookUseCase, never()).process(any());
    }
}
