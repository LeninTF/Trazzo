package trazzo.back.saasglobal.infrastructure.adapters.out.email;

import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import trazzo.back.saasglobal.application.port.out.EmailService;

/**
 * Sends transactional email via Resend's REST API (https://resend.com/docs/api-reference/emails/send-email).
 * Best-effort: when {@code resend.api-key} isn't configured (no Trazzo Resend account exists yet),
 * or the call fails for any reason, this logs a warning and returns rather than propagating —
 * a notification email must never fail the operation that triggered it.
 */
@Slf4j
@Component
public class ResendEmailAdapter implements EmailService {

    private static final String RESEND_URL = "https://api.resend.com/emails";

    private final RestClient restClient;
    private final String apiKey;
    private final String fromEmail;

    public ResendEmailAdapter(
            RestClient.Builder restClientBuilder,
            @Value("${resend.api-key:}") String apiKey,
            @Value("${resend.from:}") String fromEmail
    ) {
        this.restClient = restClientBuilder.baseUrl(RESEND_URL).build();
        this.apiKey = apiKey;
        this.fromEmail = fromEmail;
    }

    @Override
    public void send(String to, String subject, String body) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Resend API key not configured; skipping email to {} (subject: {})", to, subject);
            return;
        }
        try {
            restClient.post()
                    .header("Authorization", "Bearer " + apiKey)
                    .body(Map.of(
                            "from", fromEmail,
                            "to", List.of(to),
                            "subject", subject,
                            "html", body))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Failed to send email to {} (subject: {}): {}", to, subject, e.getMessage());
        }
    }
}
