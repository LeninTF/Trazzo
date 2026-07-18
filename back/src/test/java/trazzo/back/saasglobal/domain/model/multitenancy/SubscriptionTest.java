package trazzo.back.saasglobal.domain.model.multitenancy;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import trazzo.back.saasglobal.domain.event.SubscriptionActivatedEvent;
import trazzo.back.saasglobal.domain.exception.InvalidSubscriptionTransitionException;
import trazzo.back.saasglobal.domain.exception.TenantValidationException;

class SubscriptionTest {

    /* == createTrial == */

    @Test
    void createTrial_setsFieldsCorrectly() {
        var today = LocalDate.now();
        var sub = Subscription.createTrial("tenant-1", 2, BigDecimal.ZERO, today);

        assertNotNull(sub.getId());
        assertFalse(sub.getId().isBlank());
        assertEquals("tenant-1", sub.getTenantId());
        assertEquals(2, sub.getPlanId());
        assertEquals(BigDecimal.ZERO, sub.getPurchasePrice());
        assertEquals(today, sub.getDateStart());
        assertNull(sub.getDateEnd());
        assertEquals(SubscriptionStatus.TRIAL, sub.getStatus());
        assertTrue(sub.isTrial());
        assertFalse(sub.isActive());
        assertNotNull(sub.getCreatedAt());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void createTrial_throwsWhenTenantIdBlank(String tenantId) {
        var today = LocalDate.now();
        assertThrows(
                TenantValidationException.class,
                () -> Subscription.createTrial(tenantId, 1, BigDecimal.ZERO, today)
        );
    }

    @Test
    void createTrial_throwsWhenPlanIdNull() {
        var today = LocalDate.now();
        assertThrows(
                TenantValidationException.class,
                () -> Subscription.createTrial("tenant-1", null, BigDecimal.ZERO, today)
        );
    }

    @Test
    void createTrial_throwsWhenPurchasePriceNegative() {
        var negativePrice = new BigDecimal("-1");
        var today = LocalDate.now();
        assertThrows(
                TenantValidationException.class,
                () -> Subscription.createTrial("tenant-1", 1, negativePrice, today)
        );
    }

    @Test
    void createTrial_throwsWhenPurchasePriceNull() {
        var today = LocalDate.now();
        assertThrows(
                TenantValidationException.class,
                () -> Subscription.createTrial("tenant-1", 1, null, today)
        );
    }

    @Test
    void createTrial_throwsWhenDateStartNull() {
        assertThrows(
                TenantValidationException.class,
                () -> Subscription.createTrial("tenant-1", 1, BigDecimal.ZERO, null)
        );
    }

    /* == activate == */

    @Test
    void activate_transitionsToActiveAndSetsDateEnd() {
        var sub = Subscription.createTrial("tenant-1", 1, BigDecimal.ZERO, LocalDate.now());
        var dateEnd = LocalDate.now().plusMonths(1);
        sub.activate(dateEnd);

        assertEquals(SubscriptionStatus.ACTIVE, sub.getStatus());
        assertEquals(dateEnd, sub.getDateEnd());
        assertTrue(sub.isActive());
        assertFalse(sub.isTrial());
    }

    @Test
    void activate_recordsDomainEvent() {
        var sub = Subscription.createTrial("tenant-1", 1, BigDecimal.ZERO, LocalDate.now());
        sub.activate(LocalDate.now().plusMonths(1));

        assertEquals(1, sub.getDomainEvents().size());
        var event = assertInstanceOf(SubscriptionActivatedEvent.class, sub.getDomainEvents().getFirst());
        assertEquals(sub.getId(), event.subscriptionId());
        assertEquals("tenant-1", event.tenantId());
    }

    @Test
    void activate_throwsWhenNotTrial() {
        var sub = Subscription.createTrial("tenant-1", 1, BigDecimal.ZERO, LocalDate.now());
        sub.activate(LocalDate.now().plusMonths(1));
        var nextEnd = LocalDate.now().plusMonths(2);
        assertThrows(InvalidSubscriptionTransitionException.class,
                () -> sub.activate(nextEnd));
    }

    @Test
    void activate_throwsWhenDateEndNull() {
        var sub = Subscription.createTrial("tenant-1", 1, BigDecimal.ZERO, LocalDate.now());
        assertThrows(TenantValidationException.class, () -> sub.activate(null));
    }

    /* == suspend == */

    @Test
    void suspend_transitionsToSuspended() {
        var now = LocalDateTime.now();
        var sub = Subscription.restore("id-1", 1, "tenant-1",
                LocalDate.now(), LocalDate.now().plusMonths(1),
                SubscriptionStatus.ACTIVE, BigDecimal.TEN, null, now);
        sub.suspend();
        assertEquals(SubscriptionStatus.SUSPENDED, sub.getStatus());
    }

    @Test
    void suspend_throwsWhenNotActive() {
        var sub = Subscription.createTrial("tenant-1", 1, BigDecimal.ZERO, LocalDate.now());
        assertThrows(InvalidSubscriptionTransitionException.class, sub::suspend);
    }

    /* == cancel == */

    @Test
    void cancel_transitionsToCanceled() {
        var sub = Subscription.createTrial("tenant-1", 1, BigDecimal.ZERO, LocalDate.now());
        sub.cancel();
        assertEquals(SubscriptionStatus.CANCELED, sub.getStatus());
    }

    @Test
    void cancel_throwsWhenAlreadyCanceled() {
        var sub = Subscription.createTrial("tenant-1", 1, BigDecimal.ZERO, LocalDate.now());
        sub.cancel();
        assertThrows(InvalidSubscriptionTransitionException.class, sub::cancel);
    }

    /* == pullDomainEvents == */

    @Test
    void pullDomainEvents_clearsQueueAfterPull() {
        var sub = Subscription.createTrial("tenant-1", 1, BigDecimal.ZERO, LocalDate.now());
        sub.activate(LocalDate.now().plusMonths(1));
        assertEquals(1, sub.getDomainEvents().size());

        var events = sub.pullDomainEvents();
        assertEquals(1, events.size());
        assertTrue(sub.getDomainEvents().isEmpty());
    }

    /* == restore == */

    @Test
    void restore_setsAllFields() {
        var now = LocalDateTime.now();
        var dateStart = LocalDate.now();
        var dateEnd = dateStart.plusMonths(1);
        var sub = Subscription.restore("id-1", 2, "tenant-1",
                dateStart, dateEnd, SubscriptionStatus.ACTIVE, new BigDecimal("99.00"), "mp-preapproval-1", now);

        assertEquals("id-1", sub.getId());
        assertEquals(2, sub.getPlanId());
        assertEquals("tenant-1", sub.getTenantId());
        assertEquals(dateStart, sub.getDateStart());
        assertEquals(dateEnd, sub.getDateEnd());
        assertEquals(SubscriptionStatus.ACTIVE, sub.getStatus());
        assertEquals(new BigDecimal("99.00"), sub.getPurchasePrice());
        assertEquals("mp-preapproval-1", sub.getMpPreapprovalId());
        assertEquals(now, sub.getCreatedAt());
    }

    /* == linkMercadoPago == */

    @Test
    void linkMercadoPago_setsPreapprovalId() {
        var sub = Subscription.createTrial("tenant-1", 1, BigDecimal.ZERO, LocalDate.now());
        sub.linkMercadoPago("mp-preapproval-1");
        assertEquals("mp-preapproval-1", sub.getMpPreapprovalId());
    }

    @Test
    void linkMercadoPago_throwsWhenBlank() {
        var sub = Subscription.createTrial("tenant-1", 1, BigDecimal.ZERO, LocalDate.now());
        assertThrows(TenantValidationException.class, () -> sub.linkMercadoPago(" "));
    }
}
