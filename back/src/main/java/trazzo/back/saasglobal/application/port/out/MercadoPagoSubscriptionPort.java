package trazzo.back.saasglobal.application.port.out;

import java.math.BigDecimal;

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

    record PreapprovalCreated(String id, String status, String initPoint, String sandboxInitPoint) {}

    record PreapprovalDetails(String id, String status, String externalReference) {}

    record PaymentDetails(
            String id,
            String status,
            BigDecimal transactionAmount,
            BigDecimal netAmount,
            String externalReference
    ) {}
}
