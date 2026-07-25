package trazzo.back.saasglobal.infrastructure.adapters.out.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import trazzo.back.saasglobal.application.port.out.AppConfigPort;

@Component
public class SpringAppConfigAdapter implements AppConfigPort {

    private final String frontendUrl;
    private final String requestsNotificationEmail;

    public SpringAppConfigAdapter(
            @Value("${app.frontend-url:http://localhost:4200}") String frontendUrl,
            @Value("${trazzo.requests.notification-email:solicitudes@trazzo.pe}") String requestsNotificationEmail
    ) {
        this.frontendUrl = ensureProtocol(frontendUrl);
        this.requestsNotificationEmail = requestsNotificationEmail;
    }

    @Override
    public String frontendUrl() {
        return frontendUrl;
    }

    @Override
    public String requestsNotificationEmail() {
        return requestsNotificationEmail;
    }

    private static String ensureProtocol(String url) {
        if (url == null || url.isBlank()) {
            return "https://trazzosaas.noahtechperu.com";
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return "https://" + url;
        }
        return url;
    }
}
