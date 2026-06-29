package trazzo.back.saasglobal.domain.model.invoice;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class PaymentTransactionTest {

    private static final BigDecimal AMOUNT = BigDecimal.valueOf(118);
    private static final BigDecimal NET = BigDecimal.valueOf(100);

    @Test
    void create_setsFieldsCorrectly() {
        var before = LocalDateTime.now();
        PaymentTransaction tx = PaymentTransaction.create("t1", "sub-1", "pref-abc", AMOUNT, NET);
        var after = LocalDateTime.now();

        assertNotNull(tx.getId());
        assertEquals("t1", tx.getTenantId());
        assertEquals("sub-1", tx.getSubscriptionId());
        assertEquals("pref-abc", tx.getMpPreferenceId());
        assertNull(tx.getMpPaymentId());
        assertEquals("PENDING", tx.getPaymentStatus());
        assertFalse(tx.getCreatedAt().isBefore(before));
        assertFalse(tx.getCreatedAt().isAfter(after));
    }

    @Test
    void restore_setsAllFields() {
        var now = LocalDateTime.now();
        PaymentTransaction tx = PaymentTransaction.restore("tx-1", "t1", "sub-1",
                "pref-abc", "pay-xyz", AMOUNT, NET, "APPROVED", now);

        assertEquals("tx-1", tx.getId());
        assertEquals("APPROVED", tx.getPaymentStatus());
        assertEquals("pay-xyz", tx.getMpPaymentId());
        assertEquals(now, tx.getCreatedAt());
    }

    @Test
    void restore_defaultsPaymentStatusToPendingWhenNull() {
        var now = LocalDateTime.now();
        PaymentTransaction tx = PaymentTransaction.restore("tx-1", "t1", "sub-1",
                "pref-abc", null, AMOUNT, NET, null, now);

        assertEquals("PENDING", tx.getPaymentStatus());
    }

    @Test
    void approve_setsStatusAndPaymentId() {
        PaymentTransaction tx = PaymentTransaction.create("t1", "sub-1", "pref-abc", AMOUNT, NET);
        tx.approve("pay-xyz");

        assertEquals("APPROVED", tx.getPaymentStatus());
        assertEquals("pay-xyz", tx.getMpPaymentId());
    }

    @Test
    void reject_setsStatusRejected() {
        PaymentTransaction tx = PaymentTransaction.create("t1", "sub-1", "pref-abc", AMOUNT, NET);
        tx.reject();

        assertEquals("REJECTED", tx.getPaymentStatus());
    }

    @Test
    void create_throwsWhenTenantIdBlank() {
        assertThrows(IllegalArgumentException.class, () ->
                PaymentTransaction.create("", "sub-1", "pref-abc", AMOUNT, NET));
    }

    @Test
    void create_throwsWhenSubscriptionIdNull() {
        assertThrows(IllegalArgumentException.class, () ->
                PaymentTransaction.create("t1", null, "pref-abc", AMOUNT, NET));
    }
}
