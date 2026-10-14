package trazzo.back.saasglobal.application.port.out;

import java.math.BigDecimal;
import trazzo.back.saasglobal.domain.exception.MercadoPagoConfigurationException;

public interface MercadoPagoSubscriptionPort {

    PreapprovalCreated createPreapproval(PreapprovalRequest request);

    PreapprovalDetails getPreapproval(String preapprovalId);

    PaymentDetails getPayment(String paymentId);

    record PreapprovalRequest(
            BigDecimal amount,
            String currency,
            String billingPeriod,
            String payerEmail,
            String externalReference,
            String backUrl,
            String reason
    ) {}

    record PreapprovalCreated(String id, String status, String initPoint, String sandboxInitPoint) {

        /** The checkout URL for the configured environment; fails instead of falling back to the other one. */
        public String checkoutUrl(boolean useSandbox) {
            String url = useSandbox ? sandboxInitPoint : initPoint;
            if (url == null) {
                throw new MercadoPagoConfigurationException(
                        "Mercado Pago did not return " + (useSandbox ? "sandbox_init_point" : "init_point")
                                + " for preapproval " + id + "; check mercadopago.use-sandbox and the access token type");
            }
            return url;
        }
    }

    record PreapprovalDetails(String id, String status, String externalReference) {}

    record PaymentDetails(
            String id,
            String status,
            BigDecimal transactionAmount,
            BigDecimal netAmount,
            String externalReference
    ) {}
}
