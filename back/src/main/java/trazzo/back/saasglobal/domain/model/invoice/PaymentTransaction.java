package trazzo.back.saasglobal.domain.model.invoice;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentTransaction {

    private String id;
    private String tenantId;
    private String subscriptionId;
    private String mpPreferenceId;
    private String mpPaymentId;
    private BigDecimal amount;
    private BigDecimal netAmount;
    private String paymentStatus;
    private LocalDateTime createdAt;

    @SuppressWarnings("java:S107")
    private PaymentTransaction(String id, String tenantId, String subscriptionId,
                               String mpPreferenceId, String mpPaymentId,
                               BigDecimal amount, BigDecimal netAmount,
                               String paymentStatus, LocalDateTime createdAt) {
        this.id = id;
        this.tenantId = requireText(tenantId, "tenantId");
        this.subscriptionId = requireText(subscriptionId, "subscriptionId");
        this.mpPreferenceId = mpPreferenceId;
        this.mpPaymentId = mpPaymentId;
        this.amount = amount;
        this.netAmount = netAmount;
        this.paymentStatus = paymentStatus != null ? paymentStatus : "PENDING";
        this.createdAt = createdAt;
    }

    public static PaymentTransaction create(String tenantId, String subscriptionId,
                                            String mpPreferenceId,
                                            BigDecimal amount, BigDecimal netAmount) {
        return new PaymentTransaction(UUID.randomUUID().toString(), tenantId, subscriptionId,
                mpPreferenceId, null, amount, netAmount, "PENDING",
                LocalDateTime.now(Clock.systemDefaultZone()));
    }

    @SuppressWarnings("java:S107")
    public static PaymentTransaction restore(String id, String tenantId, String subscriptionId,
                                             String mpPreferenceId, String mpPaymentId,
                                             BigDecimal amount, BigDecimal netAmount,
                                             String paymentStatus, LocalDateTime createdAt) {
        return new PaymentTransaction(id, tenantId, subscriptionId, mpPreferenceId, mpPaymentId,
                amount, netAmount, paymentStatus, createdAt);
    }

    public void approve(String mpPaymentId) {
        this.mpPaymentId = mpPaymentId;
        this.paymentStatus = "APPROVED";
    }

    public void reject() {
        this.paymentStatus = "REJECTED";
    }

    private static String requireText(String v, String fieldName) {
        if (v == null || v.isBlank()) throw new IllegalArgumentException(fieldName + " is required");
        return v.trim();
    }
}
