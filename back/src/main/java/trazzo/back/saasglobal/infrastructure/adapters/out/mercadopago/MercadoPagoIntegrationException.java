package trazzo.back.saasglobal.infrastructure.adapters.out.mercadopago;

public class MercadoPagoIntegrationException extends RuntimeException {
    public MercadoPagoIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
