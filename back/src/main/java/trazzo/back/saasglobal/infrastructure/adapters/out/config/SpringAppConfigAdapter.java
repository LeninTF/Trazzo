package trazzo.back.saasglobal.infrastructure.adapters.out.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import trazzo.back.saasglobal.application.port.out.AppConfigPort;

@Component
public class SpringAppConfigAdapter implements AppConfigPort {

    private final String frontendUrl;
    private final String mercadoPagoBackUrl;
    private final boolean mercadoPagoUseSandbox;
    private final String requestsNotificationEmail;

    // No default for app.frontend-url: a missing value must surface as a configuration error
    // (see StartupConfigValidator), not silently fall back to a dev URL.
    public SpringAppConfigAdapter(
            @Value("${app.frontend-url}") String frontendUrl,
            @Value("${mercadopago.back-url}") String mercadoPagoBackUrl,
            @Value("${mercadopago.use-sandbox}") boolean mercadoPagoUseSandbox,
            @Value("${trazzo.requests.notification-email:solicitudes@trazzo.pe}") String requestsNotificationEmail
    ) {
        this.frontendUrl = frontendUrl;
        this.mercadoPagoBackUrl = mercadoPagoBackUrl;
        this.mercadoPagoUseSandbox = mercadoPagoUseSandbox;
        this.requestsNotificationEmail = requestsNotificationEmail;
    }

    @Override
    public String frontendUrl() {
        return frontendUrl;
    }

    @Override
    public String mercadoPagoBackUrl() {
        return mercadoPagoBackUrl;
    }

    @Override
    public boolean mercadoPagoUseSandbox() {
        return mercadoPagoUseSandbox;
    }

    @Override
    public String requestsNotificationEmail() {
        return requestsNotificationEmail;
    }
}
