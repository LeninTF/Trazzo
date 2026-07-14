package trazzo.back.saasglobal.infrastructure.adapters.out.mercadopago;

import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preapproval.PreApprovalAutoRecurringCreateRequest;
import com.mercadopago.client.preapproval.PreapprovalClient;
import com.mercadopago.client.preapproval.PreapprovalCreateRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preapproval.Preapproval;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import trazzo.back.saasglobal.application.port.out.MercadoPagoSubscriptionPort;

/**
 * SDK v3.3.0's typed request/response classes have no preapproval_plan_id field — a
 * "subscription linked to a reusable MP-side plan" isn't exposed by this version, so each
 * preapproval carries its own auto_recurring built straight from the Trazzo Plan instead of
 * referencing a separate Mercado Pago plan object.
 */
@Component
@RequiredArgsConstructor
public class MercadoPagoSubscriptionAdapter implements MercadoPagoSubscriptionPort {

    private final PreapprovalClient preapprovalClient;
    private final PaymentClient mercadoPagoPaymentClient;

    @Override
    public PreapprovalCreated createPreapproval(PreapprovalRequest request) {
        PreapprovalCreateRequest mpRequest = PreapprovalCreateRequest.builder()
                .payerEmail(request.payerEmail())
                .backUrl(request.backUrl())
                .reason(request.reason())
                .externalReference(request.externalReference())
                .autoRecurring(PreApprovalAutoRecurringCreateRequest.builder()
                        .currencyId(toMpCurrency(request.currency()))
                        .transactionAmount(request.amount())
                        .frequency(toFrequency(request.billingPeriod()))
                        .frequencyType("months")
                        .build())
                .build();
        try {
            Preapproval created = preapprovalClient.create(mpRequest);
            return new PreapprovalCreated(
                    created.getId(), created.getStatus(), created.getInitPoint(), created.getSandboxInitPoint());
        } catch (MPException | MPApiException e) {
            throw new MercadoPagoIntegrationException("Failed to create Mercado Pago preapproval", e);
        }
    }

    @Override
    public PreapprovalDetails getPreapproval(String preapprovalId) {
        try {
            Preapproval preapproval = preapprovalClient.get(preapprovalId);
            return new PreapprovalDetails(
                    preapproval.getId(), preapproval.getStatus(), preapproval.getExternalReference());
        } catch (MPException | MPApiException e) {
            throw new MercadoPagoIntegrationException("Failed to fetch Mercado Pago preapproval " + preapprovalId, e);
        }
    }

    @Override
    public PaymentDetails getPayment(String paymentId) {
        try {
            Payment payment = mercadoPagoPaymentClient.get(Long.valueOf(paymentId));
            return new PaymentDetails(
                    String.valueOf(payment.getId()),
                    payment.getStatus(),
                    payment.getTransactionAmount(),
                    payment.getNetAmount(),
                    payment.getExternalReference());
        } catch (MPException | MPApiException e) {
            throw new MercadoPagoIntegrationException("Failed to fetch Mercado Pago payment " + paymentId, e);
        }
    }

    private static String toMpCurrency(String trazzoCurrency) {
        if (trazzoCurrency == null) {
            return "PEN";
        }
        return switch (trazzoCurrency.toUpperCase()) {
            case "DOLAR" -> "USD";
            case "EURO" -> "EUR";
            default -> "PEN";
        };
    }

    private static int toFrequency(String billingPeriod) {
        if (billingPeriod == null) {
            return 1;
        }
        String upper = billingPeriod.toUpperCase();
        boolean annual = upper.contains("ANNUAL") || upper.contains("ANUAL") || upper.contains("YEARLY");
        return annual ? 12 : 1;
    }
}
