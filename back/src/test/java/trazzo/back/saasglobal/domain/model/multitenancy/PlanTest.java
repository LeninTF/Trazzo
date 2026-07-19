package trazzo.back.saasglobal.domain.model.multitenancy;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PlanTest {

    @Test
    void create_setsFieldsCorrectly() {
        var before = LocalDateTime.now();
        var plan = Plan.create("Basic", new BigDecimal("29.99"), new BigDecimal("299.99"), "SOLES", "MONTHLY");
        var after = LocalDateTime.now();

        assertNull(plan.getId());
        assertEquals("Basic", plan.getName());
        assertEquals(new BigDecimal("29.99"), plan.getPrice());
        assertEquals(new BigDecimal("299.99"), plan.getPriceAnnual());
        assertEquals("SOLES", plan.getCurrency());
        assertEquals("MONTHLY", plan.getBillingPeriod());
        assertTrue(plan.isActive());
        assertNull(plan.getDeletedAt());
        assertFalse(plan.getCreatedAt().isBefore(before));
        assertFalse(plan.getCreatedAt().isAfter(after));
    }

    @Test
    void create_zeroPriceIsAllowed() {
        assertDoesNotThrow(() -> Plan.create("Free", BigDecimal.ZERO, BigDecimal.ZERO, "SOLES", null));
    }

    @Test
    void create_nullPriceAnnualIsAllowed() {
        var plan = Plan.create("Basic", BigDecimal.ONE, null, "SOLES", "MONTHLY");
        assertNull(plan.getPriceAnnual());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void create_throwsWhenNameBlank(String name) {
        assertThrows(IllegalArgumentException.class,
                () -> Plan.create(name, BigDecimal.ONE, BigDecimal.ONE, "SOLES", "MONTHLY"));
    }

    @Test
    void create_throwsWhenPriceNull() {
        assertThrows(IllegalArgumentException.class,
                () -> Plan.create("Basic", null, BigDecimal.ONE, "SOLES", "MONTHLY"));
    }

    @Test
    void create_throwsWhenPriceNegative() {
        var negative = new BigDecimal("-1");
        assertThrows(IllegalArgumentException.class,
                () -> Plan.create("Basic", negative, BigDecimal.ONE, "SOLES", "MONTHLY"));
    }

    @Test
    void create_throwsWhenPriceAnnualNegative() {
        var negative = new BigDecimal("-1");
        assertThrows(IllegalArgumentException.class,
                () -> Plan.create("Basic", BigDecimal.ONE, negative, "SOLES", "MONTHLY"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void create_throwsWhenCurrencyBlank(String currency) {
        assertThrows(IllegalArgumentException.class,
                () -> Plan.create("Basic", BigDecimal.ONE, BigDecimal.ONE, currency, "MONTHLY"));
    }

    @Test
    void restore_setsAllFields() {
        var now = LocalDateTime.now();
        var plan = Plan.restore(5, "Pro", new BigDecimal("99.00"), new BigDecimal("999.00"), "DOLAR", "ANNUAL",
                false, now, now, now);

        assertEquals(5, plan.getId());
        assertEquals("Pro", plan.getName());
        assertEquals(new BigDecimal("99.00"), plan.getPrice());
        assertEquals(new BigDecimal("999.00"), plan.getPriceAnnual());
        assertEquals("DOLAR", plan.getCurrency());
        assertEquals("ANNUAL", plan.getBillingPeriod());
        assertFalse(plan.isActive());
        assertEquals(now, plan.getCreatedAt());
        assertEquals(now, plan.getDeletedAt());
    }

    @Test
    void deactivate_setsIsActiveFalse() {
        var plan = Plan.create("Basic", BigDecimal.ONE, BigDecimal.ONE, "SOLES", "MONTHLY");
        assertTrue(plan.isActive());
        plan.deactivate();
        assertFalse(plan.isActive());
    }

    @Test
    void activate_setsIsActiveTrue() {
        var now = LocalDateTime.now();
        var plan = Plan.restore(1, "Basic", BigDecimal.ONE, BigDecimal.ONE, "SOLES", "MONTHLY", false, now, now, null);
        assertFalse(plan.isActive());
        plan.activate();
        assertTrue(plan.isActive());
    }

    @Test
    void update_changesFields() {
        var plan = Plan.create("Basic", BigDecimal.valueOf(99), BigDecimal.valueOf(999), "SOLES", "MONTHLY");
        plan.update("Pro", BigDecimal.valueOf(199), BigDecimal.valueOf(1999), "DOLAR", "ANNUAL");
        assertEquals("Pro", plan.getName());
        assertEquals(BigDecimal.valueOf(199), plan.getPrice());
        assertEquals(BigDecimal.valueOf(1999), plan.getPriceAnnual());
        assertEquals("DOLAR", plan.getCurrency());
        assertEquals("ANNUAL", plan.getBillingPeriod());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void update_throwsWhenNameBlank(String name) {
        var plan = Plan.create("Basic", BigDecimal.ONE, BigDecimal.ONE, "SOLES", "MONTHLY");
        assertThrows(IllegalArgumentException.class,
                () -> plan.update(name, BigDecimal.ONE, BigDecimal.ONE, "SOLES", "MONTHLY"));
    }

    @Test
    void update_throwsWhenPriceNegative() {
        var plan = Plan.create("Basic", BigDecimal.ONE, BigDecimal.ONE, "SOLES", "MONTHLY");
        var negative = new BigDecimal("-1");
        assertThrows(IllegalArgumentException.class,
                () -> plan.update("Basic", negative, BigDecimal.ONE, "SOLES", "MONTHLY"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void update_throwsWhenCurrencyBlank(String currency) {
        var plan = Plan.create("Basic", BigDecimal.ONE, BigDecimal.ONE, "SOLES", "MONTHLY");
        assertThrows(IllegalArgumentException.class,
                () -> plan.update("Basic", BigDecimal.ONE, BigDecimal.ONE, currency, "MONTHLY"));
    }

    @Test
    void delete_setsIsActiveAndDeletedAt() {
        var plan = Plan.create("Basic", BigDecimal.ONE, BigDecimal.ONE, "SOLES", "MONTHLY");
        assertTrue(plan.isActive());
        assertNull(plan.getDeletedAt());
        plan.delete();
        assertFalse(plan.isActive());
        assertNotNull(plan.getDeletedAt());
    }
}
