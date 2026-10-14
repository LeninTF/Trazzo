package trazzo.back.saasglobal.domain.exception;

/** Mercado Pago did not return the checkout URL this deployment is configured to use (sandbox vs production). */
public class MercadoPagoConfigurationException extends RuntimeException {
    public MercadoPagoConfigurationException(String message) {
        super(message);
    }
}
