package trazzo.back.saasglobal.domain.model.invoice;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InvoiceDetails {

    private Integer id;
    private String invoiceId;
    private String subscriptionId;
    private String description;
    private Integer quantity;
    private BigDecimal unitValue;
    private BigDecimal unitPrice;
    private BigDecimal unitTax;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal total;
    private LocalDateTime createdAt;

    @SuppressWarnings("java:S107")
    private InvoiceDetails(Integer id, String invoiceId, String subscriptionId, String description,
                           Integer quantity, BigDecimal unitValue, BigDecimal unitPrice,
                           BigDecimal unitTax, BigDecimal subtotal, BigDecimal taxAmount,
                           BigDecimal total, LocalDateTime createdAt) {
        this.id = id;
        this.invoiceId = requireText(invoiceId, "invoiceId");
        this.subscriptionId = subscriptionId;
        this.description = requireText(description, "description");
        this.quantity = quantity;
        this.unitValue = unitValue;
        this.unitPrice = unitPrice;
        this.unitTax = unitTax;
        this.subtotal = subtotal;
        this.taxAmount = taxAmount;
        this.total = total;
        this.createdAt = createdAt;
    }

    @SuppressWarnings("java:S107")
    public static InvoiceDetails create(String invoiceId, String subscriptionId, String description,
                                        Integer quantity, BigDecimal unitValue, BigDecimal unitPrice,
                                        BigDecimal unitTax, BigDecimal taxAmount, BigDecimal total) {
        BigDecimal subtotal = unitValue.multiply(BigDecimal.valueOf(quantity));
        return new InvoiceDetails(null, invoiceId, subscriptionId, description, quantity,
                unitValue, unitPrice, unitTax, subtotal, taxAmount, total,
                LocalDateTime.now(Clock.systemDefaultZone()));
    }

    @SuppressWarnings("java:S107")
    public static InvoiceDetails restore(Integer id, String invoiceId, String subscriptionId,
                                         String description, Integer quantity, BigDecimal unitValue,
                                         BigDecimal unitPrice, BigDecimal unitTax, BigDecimal subtotal,
                                         BigDecimal taxAmount, BigDecimal total, LocalDateTime createdAt) {
        return new InvoiceDetails(id, invoiceId, subscriptionId, description, quantity,
                unitValue, unitPrice, unitTax, subtotal, taxAmount, total, createdAt);
    }

    private static String requireText(String v, String fieldName) {
        if (v == null || v.isBlank()) throw new IllegalArgumentException(fieldName + " is required");
        return v.trim();
    }
}
