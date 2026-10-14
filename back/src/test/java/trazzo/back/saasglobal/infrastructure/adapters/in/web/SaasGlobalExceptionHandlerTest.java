package trazzo.back.saasglobal.infrastructure.adapters.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import trazzo.back.saasglobal.domain.exception.MercadoPagoConfigurationException;

class SaasGlobalExceptionHandlerTest {

    private final SaasGlobalExceptionHandler handler = new SaasGlobalExceptionHandler();

    @Test
    void mercadoPagoConfigurationError_mapsTo503WithoutLeakingTheInternalMessage() {
        var response = handler.handleMercadoPagoConfiguration(
                new MercadoPagoConfigurationException("Mercado Pago did not return sandbox_init_point for preapproval abc"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().toString()).doesNotContain("sandbox_init_point").doesNotContain("abc");
    }
}
