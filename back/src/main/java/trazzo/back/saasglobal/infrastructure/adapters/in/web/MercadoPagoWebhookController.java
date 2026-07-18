package trazzo.back.saasglobal.infrastructure.adapters.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadopago.exceptions.MPInvalidWebhookSignatureException;
import com.mercadopago.webhook.WebhookSignatureValidator;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import trazzo.back.saasglobal.application.dto.command.MercadoPagoWebhookCommand;
import trazzo.back.saasglobal.application.port.in.ProcessMercadoPagoWebhookUseCase;
import trazzo.back.saasglobal.infrastructure.adapters.in.web.dto.MercadoPagoWebhookEnvelope;

/**
 * Public receiver for Mercado Pago's async payment/subscription notifications (see
 * SecurityConfig permitAll — Mercado Pago cannot present a Bearer token, so this endpoint
 * authenticates the caller via WebhookSignatureValidator instead of Spring Security).
 */
@RestController
@RequestMapping("/webhooks/mercadopago")
@Slf4j
public class MercadoPagoWebhookController {

    private final ProcessMercadoPagoWebhookUseCase webhookUseCase;
    private final ObjectMapper objectMapper;
    private final String webhookSecret;

    public MercadoPagoWebhookController(
            ProcessMercadoPagoWebhookUseCase webhookUseCase,
            ObjectMapper objectMapper,
            @Value("${mercadopago.webhook-secret:}") String webhookSecret
    ) {
        this.webhookUseCase = webhookUseCase;
        this.objectMapper = objectMapper;
        this.webhookSecret = webhookSecret;
    }

    @PostMapping
    public ResponseEntity<Void> receive(
            HttpServletRequest request,
            @RequestHeader(value = "x-signature", required = false) String xSignature,
            @RequestHeader(value = "x-request-id", required = false) String xRequestId
    ) throws IOException {
        String rawPayload = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        MercadoPagoWebhookEnvelope envelope;
        try {
            envelope = objectMapper.readValue(rawPayload, MercadoPagoWebhookEnvelope.class);
        } catch (IOException e) {
            log.warn("Malformed Mercado Pago webhook payload", e);
            return ResponseEntity.badRequest().build();
        }
        String dataId = envelope.data() != null ? envelope.data().id() : null;

        // Never process a webhook without a configured secret, even in dev — an unverifiable
        // notification must not be trusted just because the secret hasn't been set up yet.
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.error("mercadopago.webhook-secret is not configured — rejecting notificationId={}", envelope.id());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            WebhookSignatureValidator.validate(xSignature, xRequestId, dataId, webhookSecret);
        } catch (MPInvalidWebhookSignatureException e) {
            log.warn("Invalid Mercado Pago webhook signature for notificationId={}: {}", envelope.id(), e.getReason());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        webhookUseCase.process(new MercadoPagoWebhookCommand(
                envelope.id(), envelope.type(), envelope.action(), dataId, rawPayload));
        return ResponseEntity.ok().build();
    }
}
