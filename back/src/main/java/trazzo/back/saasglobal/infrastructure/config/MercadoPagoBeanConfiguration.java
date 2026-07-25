package trazzo.back.saasglobal.infrastructure.config;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preapproval.PreapprovalClient;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoBeanConfiguration.class);

    private final String accessToken;

    public MercadoPagoBeanConfiguration(@Value("${mercadopago.access-token:}") String accessToken) {
        this.accessToken = accessToken;
    }

    @PostConstruct
    void configureAccessToken() {
        if (accessToken != null && !accessToken.isBlank()) {
            MercadoPagoConfig.setAccessToken(accessToken);
            log.info("MercadoPago access token configured successfully (length={})", accessToken.length());
        } else {
            log.warn("MercadoPago access token is EMPTY or NOT SET — API calls will fail with 401. "
                    + "Set MERCADOPAGO_ACCESS_TOKEN environment variable or mercadopago.access-token property.");
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
