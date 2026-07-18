package trazzo.back.saasglobal.infrastructure.config;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preapproval.PreapprovalClient;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MercadoPagoConfig.setAccessToken is a static, process-wide setting (the SDK has no
 * per-instance client credentials) — safe here because Trazzo has a single Mercado Pago
 * collector account; tenants are payers, not separate MP accounts.
 */
@Configuration
public class MercadoPagoBeanConfiguration {

    private final String accessToken;

    public MercadoPagoBeanConfiguration(@Value("${mercadopago.access-token:}") String accessToken) {
        this.accessToken = accessToken;
    }

    @PostConstruct
    void configureAccessToken() {
        if (accessToken != null && !accessToken.isBlank()) {
            MercadoPagoConfig.setAccessToken(accessToken);
        }
    }

    @Bean
    PreapprovalClient preapprovalClient() {
        return new PreapprovalClient();
    }

    @Bean
    PaymentClient mercadoPagoPaymentClient() {
        return new PaymentClient();
    }
}
