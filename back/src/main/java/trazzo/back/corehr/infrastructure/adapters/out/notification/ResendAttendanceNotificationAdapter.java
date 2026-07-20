package trazzo.back.corehr.infrastructure.adapters.out.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import trazzo.back.corehr.application.port.out.AttendanceNotificationPort;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ResendAttendanceNotificationAdapter implements AttendanceNotificationPort {

    private static final String RESEND_URL = "https://api.resend.com/emails";

    private final RestClient restClient;
    private final String apiKey;
    private final String fromEmail;

    public ResendAttendanceNotificationAdapter(
            @Value("${resend.api-key:}") String apiKey,
            @Value("${resend.from-email:notificaciones@trazzo.pe}") String fromEmail
    ) {
        this.apiKey = apiKey;
        this.fromEmail = fromEmail;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        this.restClient = RestClient.builder()
                .baseUrl(RESEND_URL)
                .requestFactory(factory)
                .build();
    }

    @Override
    public void notifyTardanza(String toEmail, String workerName, int minutesLate, LocalDate date) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Resend API key not configured; skipping tardanza notification");
            return;
        }
        if (toEmail == null) {
            log.warn("No email provided for tardanza notification; skipping");
            return;
        }
        try {
            String subject = "Notificación de Tardanza - " + date;
            String htmlBody = buildTardanzaHtml(workerName, minutesLate, date);

            restClient.post()
                    .header("Authorization", "Bearer " + apiKey)
                    .body(Map.of(
                            "from", fromEmail,
                            "to", List.of(toEmail),
                            "subject", subject,
                            "html", htmlBody))
                    .retrieve()
                    .toBodilessEntity();

            log.info("Tardanza notification sent to {} ({} minutes late)", toEmail, minutesLate);
        } catch (Exception e) {
            log.warn("Failed to send tardanza notification to {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildTardanzaHtml(String workerName, int minutesLate, LocalDate date) {
        return """
                <html>
                <body style="font-family: Arial, sans-serif; padding: 20px;">
                    <h2 style="color: #e74c3c;">Notificación de Tardanza</h2>
                    <p>Estimado(a) <strong>%s</strong>,</p>
                    <p>Se ha registrado una tardanza en su marcación de asistencia:</p>
                    <ul>
                        <li><strong>Fecha:</strong> %s</li>
                        <li><strong>Minutos de tardanza:</strong> %d minutos</li>
                    </ul>
                    <p>Por favor, verifique su horario asignado.</p>
                    <p style="color: #7f8c8d; font-size: 12px;">Este es un correo automático, no responda a este mensaje.</p>
                </body>
                </html>
                """.formatted(workerName != null ? workerName : "Trabajador", date, minutesLate);
    }
}
