package trazzo.back.saasglobal.domain.model.invoice;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class InvoiceDetailsTest {

    private static final BigDecimal UNIT_VALUE = BigDecimal.valueOf(50);
    private static final BigDecimal UNIT_PRICE = BigDecimal.valueOf(59);
    private static final BigDecimal UNIT_TAX = BigDecimal.valueOf(9);
    private static final BigDecimal TAX = BigDecimal.valueOf(9);
    private static final BigDecimal TOTAL = BigDecimal.valueOf(59);

    @Test
    void create_setsFieldsAndComputesSubtotal() {
        var before = LocalDateTime.now();
        InvoiceDetails d = InvoiceDetails.create("inv-1", "sub-1", "Plan Basic",
                2, UNIT_VALUE, UNIT_PRICE, UNIT_TAX, TAX, TOTAL);
        var after = LocalDateTime.now();

        assertNull(d.getId());
        assertEquals("inv-1", d.getInvoiceId());
        assertEquals("sub-1", d.getSubscriptionId());
        assertEquals("Plan Basic", d.getDescription());
        assertEquals(2, d.getQuantity());
        assertEquals(0, BigDecimal.valueOf(100).compareTo(d.getSubtotal()));
        assertFalse(d.getCreatedAt().isBefore(before));
        assertFalse(d.getCreatedAt().isAfter(after));
    }

    @Test
    void restore_setsAllFields() {
        var now = LocalDateTime.now();
        InvoiceDetails d = InvoiceDetails.restore(3, "inv-1", "sub-1", "Plan Pro",
                1, UNIT_VALUE, UNIT_PRICE, UNIT_TAX, UNIT_VALUE, TAX, TOTAL, now);

        assertEquals(3, d.getId());
        assertEquals("Plan Pro", d.getDescription());
        assertEquals(1, d.getQuantity());
        assertEquals(now, d.getCreatedAt());
    }

    @Test
    void create_throwsWhenInvoiceIdBlank() {
        assertThrows(IllegalArgumentException.class, () ->
                InvoiceDetails.create("", "sub-1", "Plan Basic",
                        1, UNIT_VALUE, UNIT_PRICE, UNIT_TAX, TAX, TOTAL));
    }

    @Test
    void create_throwsWhenDescriptionNull() {
        assertThrows(IllegalArgumentException.class, () ->
                InvoiceDetails.create("inv-1", "sub-1", null,
                        1, UNIT_VALUE, UNIT_PRICE, UNIT_TAX, TAX, TOTAL));
    }
}
