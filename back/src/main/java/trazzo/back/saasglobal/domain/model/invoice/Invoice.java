package trazzo.back.saasglobal.domain.model.invoice;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Invoice {

    private String id;
    private String pdfUrl;
    private String tenantId;
    private String paymentTransactionId;
    private String invoiceSeries;
    private String consecutiveNumber;
    private String voucherType;
    private String issuerTaxId;
    private String issuerName;
    private String issuerTaxAddress;
    private String clientTaxId;
    private String clientName;
    private String clientAddress;
    private String currencyCode;
    private BigDecimal exchangeRate;
    private BigDecimal subTotal;
    private BigDecimal taxAmount;
    private BigDecimal total;
    private BigDecimal totalDiscount;
    private String paymentStatus;
    private String notes;
    private LocalDate expirationDate;
    private LocalDateTime createdAt;

    @SuppressWarnings("java:S107")
    private Invoice(String id, String pdfUrl, String tenantId, String paymentTransactionId,
                    String invoiceSeries, String consecutiveNumber, String voucherType,
                    String issuerTaxId, String issuerName, String issuerTaxAddress,
                    String clientTaxId, String clientName, String clientAddress,
                    String currencyCode, BigDecimal exchangeRate,
                    BigDecimal subTotal, BigDecimal taxAmount, BigDecimal total,
                    BigDecimal totalDiscount, String paymentStatus, String notes,
                    LocalDate expirationDate, LocalDateTime createdAt) {
        this.id = id;
        this.pdfUrl = pdfUrl;
        this.tenantId = requireText(tenantId, "tenantId");
        this.paymentTransactionId = requireText(paymentTransactionId, "paymentTransactionId");
        this.invoiceSeries = requireText(invoiceSeries, "invoiceSeries");
        this.consecutiveNumber = requireText(consecutiveNumber, "consecutiveNumber");
        this.voucherType = requireText(voucherType, "voucherType");
        this.issuerTaxId = requireText(issuerTaxId, "issuerTaxId");
        this.issuerName = requireText(issuerName, "issuerName");
        this.issuerTaxAddress = requireText(issuerTaxAddress, "issuerTaxAddress");
        this.clientTaxId = requireText(clientTaxId, "clientTaxId");
        this.clientName = requireText(clientName, "clientName");
        this.clientAddress = clientAddress;
        this.currencyCode = currencyCode;
        this.exchangeRate = exchangeRate;
        this.subTotal = subTotal;
        this.taxAmount = taxAmount;
        this.total = total;
        this.totalDiscount = totalDiscount;
        this.paymentStatus = paymentStatus != null ? paymentStatus : "PENDING";
        this.notes = notes;
        this.expirationDate = expirationDate;
        this.createdAt = createdAt;
    }

    @SuppressWarnings("java:S107")
    public static Invoice create(String tenantId, String paymentTransactionId,
                                 String invoiceSeries, String consecutiveNumber, String voucherType,
                                 String issuerTaxId, String issuerName, String issuerTaxAddress,
                                 String clientTaxId, String clientName, String clientAddress,
                                 String currencyCode, BigDecimal subTotal,
                                 BigDecimal taxAmount, BigDecimal total) {
        return new Invoice(UUID.randomUUID().toString(), null, tenantId, paymentTransactionId,
                invoiceSeries, consecutiveNumber, voucherType, issuerTaxId, issuerName,
                issuerTaxAddress, clientTaxId, clientName, clientAddress,
                currencyCode, null, subTotal, taxAmount, total, null, "PENDING", null, null,
                LocalDateTime.now(Clock.systemDefaultZone()));
    }

    @SuppressWarnings("java:S107")
    public static Invoice restore(String id, String pdfUrl, String tenantId,
                                  String paymentTransactionId, String invoiceSeries,
                                  String consecutiveNumber, String voucherType,
                                  String issuerTaxId, String issuerName, String issuerTaxAddress,
                                  String clientTaxId, String clientName, String clientAddress,
                                  String currencyCode, BigDecimal exchangeRate,
                                  BigDecimal subTotal, BigDecimal taxAmount, BigDecimal total,
                                  BigDecimal totalDiscount, String paymentStatus, String notes,
                                  LocalDate expirationDate, LocalDateTime createdAt) {
        return new Invoice(id, pdfUrl, tenantId, paymentTransactionId, invoiceSeries,
                consecutiveNumber, voucherType, issuerTaxId, issuerName, issuerTaxAddress,
                clientTaxId, clientName, clientAddress, currencyCode, exchangeRate,
                subTotal, taxAmount, total, totalDiscount, paymentStatus, notes,
                expirationDate, createdAt);
    }

    private static String requireText(String v, String fieldName) {
        if (v == null || v.isBlank()) throw new IllegalArgumentException(fieldName + " is required");
        return v.trim();
    }
}
