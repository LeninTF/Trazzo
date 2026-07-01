package trazzo.back.saasglobal.domain.model.invoice;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class PaymentWebhooksLogTest {

    @Test
    void create_setsFieldsCorrectly() {
        var before = LocalDateTime.now();
        PaymentWebhooksLog log = PaymentWebhooksLog.create("evt-1", "mp-evt-1", "payment.created", "{\"id\":1}");
        var after = LocalDateTime.now();

        assertEquals("evt-1", log.getId());
        assertEquals("mp-evt-1", log.getMpEventId());
        assertEquals("payment.created", log.getAction());
        assertEquals("{\"id\":1}", log.getRawPayload());
        assertFalse(log.isProcessed());
        assertFalse(log.getReceivedAt().isBefore(before));
        assertFalse(log.getReceivedAt().isAfter(after));
    }

    @Test
    void restore_setsAllFields() {
        var now = LocalDateTime.now();
        PaymentWebhooksLog log = PaymentWebhooksLog.restore("evt-1", "mp-evt-1",
                "payment.updated", "{}", true, now);

        assertEquals("evt-1", log.getId());
        assertTrue(log.isProcessed());
        assertEquals(now, log.getReceivedAt());
    }

    @Test
    void markProcessed_setsProcessedTrue() {
        PaymentWebhooksLog log = PaymentWebhooksLog.create("evt-1", "mp-evt-1", "action", "{}");
        assertFalse(log.isProcessed());

        log.markProcessed();

        assertTrue(log.isProcessed());
    }

    @Test
    void create_throwsWhenIdBlank() {
        assertThrows(IllegalArgumentException.class, () ->
                PaymentWebhooksLog.create("", "mp-evt-1", "action", "{}"));
    }

    @Test
    void create_throwsWhenIdNull() {
        assertThrows(IllegalArgumentException.class, () ->
                PaymentWebhooksLog.create(null, "mp-evt-1", "action", "{}"));
    }
}
