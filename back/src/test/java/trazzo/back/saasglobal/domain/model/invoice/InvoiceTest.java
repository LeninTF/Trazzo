package trazzo.back.saasglobal.domain.model.invoice;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class InvoiceTest {

    private static final BigDecimal SUBTOTAL = BigDecimal.valueOf(100);
    private static final BigDecimal TAX = BigDecimal.valueOf(18);
    private static final BigDecimal TOTAL = BigDecimal.valueOf(118);

    @Test
    void create_setsFieldsCorrectly() {
        var before = LocalDateTime.now();
        Invoice inv = Invoice.create("t1", "tx1", "F001", "001", "FACTURA",
                "20111111111", "Empresa SAC", "Av. Lima 123",
                "20222222222", "Cliente SAC", "Av. Arequipa 456",
                "PEN", SUBTOTAL, TAX, TOTAL);
        var after = LocalDateTime.now();

        assertNotNull(inv.getId());
        assertEquals("t1", inv.getTenantId());
        assertEquals("tx1", inv.getPaymentTransactionId());
        assertEquals("F001", inv.getInvoiceSeries());
        assertEquals("001", inv.getConsecutiveNumber());
        assertEquals("FACTURA", inv.getVoucherType());
        assertEquals("20111111111", inv.getIssuerTaxId());
        assertEquals("PENDING", inv.getPaymentStatus());
        assertNull(inv.getPdfUrl());
        assertFalse(inv.getCreatedAt().isBefore(before));
        assertFalse(inv.getCreatedAt().isAfter(after));
    }

    @Test
    void restore_setsAllFields() {
        var now = LocalDateTime.now();
        Invoice inv = Invoice.restore("id1", "url.pdf", "t1", "tx1", "F001", "001", "FACTURA",
                "20111111111", "Empresa SAC", "Av. Lima",
                "20222222222", "Cliente SAC", "Av. Arequipa",
                "PEN", BigDecimal.ONE, SUBTOTAL, TAX, TOTAL,
                BigDecimal.ZERO, "PAID", "sin notas", null, now);

        assertEquals("id1", inv.getId());
        assertEquals("url.pdf", inv.getPdfUrl());
        assertEquals("PAID", inv.getPaymentStatus());
        assertEquals("sin notas", inv.getNotes());
        assertEquals(now, inv.getCreatedAt());
    }

    @Test
    void restore_defaultsPaymentStatusToPendingWhenNull() {
        var now = LocalDateTime.now();
        Invoice inv = Invoice.restore("id1", null, "t1", "tx1", "F001", "001", "FACTURA",
                "20111111111", "Empresa SAC", "Av. Lima",
                "20222222222", "Cliente SAC", "Av. Arequipa",
                "PEN", null, SUBTOTAL, TAX, TOTAL, null, null, null, null, now);

        assertEquals("PENDING", inv.getPaymentStatus());
    }

    @Test
    void create_allowsNullPaymentTransactionId() {
        Invoice inv = Invoice.create("t1", null, "F001", "001", "FACTURA",
                "20111111111", "Empresa SAC", "Av. Lima 123",
                "20222222222", "Cliente SAC", "Av. Arequipa 456",
                "PEN", SUBTOTAL, TAX, TOTAL);

        assertNull(inv.getPaymentTransactionId());
    }

    @Test
    void create_throwsWhenTenantIdBlank() {
        assertThrows(IllegalArgumentException.class, () ->
                Invoice.create("", "tx1", "F001", "001", "FACTURA",
                        "20111111111", "Empresa SAC", "Av. Lima",
                        "20222222222", "Cliente", "Dir", "PEN", SUBTOTAL, TAX, TOTAL));
    }

    @Test
    void create_throwsWhenIssuerTaxIdNull() {
        assertThrows(IllegalArgumentException.class, () ->
                Invoice.create("t1", "tx1", "F001", "001", "FACTURA",
                        null, "Empresa SAC", "Av. Lima",
                        "20222222222", "Cliente", "Dir", "PEN", SUBTOTAL, TAX, TOTAL));
    }
}
