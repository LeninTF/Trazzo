package trazzo.back.saasglobal.domain.model.multitenancy;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import trazzo.back.saasglobal.domain.event.TenantActivatedEvent;
import trazzo.back.saasglobal.domain.exception.InvalidTenantTransitionException;
import trazzo.back.saasglobal.domain.exception.TenantAlreadyActivatedException;
import trazzo.back.saasglobal.domain.exception.TenantValidationException;

class TenantTest {

    private static TenantSettings validSettings() {
        return TenantSettings.of(null, "tenant_testdb");
    }

    /* == createTrial == */

    @Test
    void createTrial_setsFieldsCorrectly() {
        var before = LocalDateTime.now();
        var settings = validSettings();
        var tenant = Tenant.createTrial("acme", 1, 10, settings, null);
        var after = LocalDateTime.now();

        assertNotNull(tenant.getId());
        assertFalse(tenant.getId().isBlank());
        assertEquals("acme", tenant.getSubDomain());
        assertEquals(1, tenant.getPlanId());
        assertEquals(10, tenant.getHoldingId());
        assertSame(settings, tenant.getSettings());
        assertNull(tenant.getBranding());
        assertNull(tenant.getActivatedAt());
        assertFalse(tenant.isActivated());
        assertNotNull(tenant.getCreatedAt());
        assertFalse(tenant.getCreatedAt().isBefore(before));
        assertFalse(tenant.getCreatedAt().isAfter(after));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void createTrial_throwsWhenSubDomainBlank(String subDomain) {
        assertThrows(
                TenantValidationException.class,
                () -> Tenant.createTrial(subDomain, 1, null, validSettings(), null)
        );
    }

    @Test
    void createTrial_throwsWhenPlanIdNull() {
        assertThrows(
                TenantValidationException.class,
                () -> Tenant.createTrial("acme", null, null, validSettings(), null)
        );
    }

    @Test
    void createTrial_throwsWhenSettingsNull() {
        assertThrows(
                TenantValidationException.class,
                () -> Tenant.createTrial("acme", 1, null, null, null)
        );
    }

    @Test
    void createTrial_trimsSubDomainWhitespace() {
        var tenant = Tenant.createTrial("  acme  ", 1, null, validSettings(), null);
        assertEquals("acme", tenant.getSubDomain());
    }

    /* == createPending == */

    @Test
    void createPending_setsFieldsWithoutSettings() {
        var tenant = Tenant.createPending("beta", 2, 5);
        assertNotNull(tenant.getId());
        assertEquals("beta", tenant.getSubDomain());
        assertEquals(2, tenant.getPlanId());
        assertNull(tenant.getSettings());
        assertFalse(tenant.hasSettings());
        assertFalse(tenant.isActivated());
    }

    /* == activate == */

    @Test
    void activate_setsActivatedAtAndRecordsEvent() {
        var before = LocalDateTime.now();
        var tenant = Tenant.createTrial("acme", 1, null, validSettings(), null);
        tenant.activate();
        var after = LocalDateTime.now();

        assertTrue(tenant.isActivated());
        assertNotNull(tenant.getActivatedAt());
        assertFalse(tenant.getActivatedAt().isBefore(before));
        assertFalse(tenant.getActivatedAt().isAfter(after));

        assertEquals(1, tenant.getDomainEvents().size());
        var event = assertInstanceOf(TenantActivatedEvent.class, tenant.getDomainEvents().getFirst());
        assertEquals(tenant.getId(), event.tenantId());
        assertEquals("acme", event.subDomain());
    }

    @Test
    void activate_throwsWhenAlreadyActivated() {
        var tenant = Tenant.createTrial("acme", 1, null, validSettings(), null);
        tenant.activate();
        assertThrows(TenantAlreadyActivatedException.class, tenant::activate);
    }

    @Test
    void activate_throwsWhenSettingsNull() {
        var tenant = Tenant.createPending("beta", 2, null);
        assertThrows(TenantValidationException.class, tenant::activate);
    }

    /* == assignSettings == */

    @Test
    void assignSettings_updatesSettings() {
        var tenant = Tenant.createPending("beta", 2, null);
        var settings = validSettings();
        tenant.assignSettings(settings);
        assertSame(settings, tenant.getSettings());
        assertTrue(tenant.hasSettings());
    }

    @Test
    void assignSettings_throwsWhenAlreadyActivated() {
        var tenant = Tenant.createTrial("acme", 1, null, validSettings(), null);
        tenant.activate();
        assertThrows(TenantAlreadyActivatedException.class,
                () -> tenant.assignSettings(validSettings()));
    }

    @Test
    void assignSettings_throwsWhenSettingsNull() {
        var tenant = Tenant.createPending("beta", 2, null);
        assertThrows(TenantValidationException.class, () -> tenant.assignSettings(null));
    }

    /* == assignBranding == */

    @Test
    void assignBranding_updatesBrandingField() {
        var tenant = Tenant.createTrial("acme", 1, null, validSettings(), null);
        var branding = TenantBranding.of(null, "http://logo", "slogan", "#fff", "#000");
        tenant.assignBranding(branding);
        assertSame(branding, tenant.getBranding());
    }

    /* == delete == */

    @Test
    void delete_setsDeletedAt() {
        var tenant = Tenant.createTrial("acme", 1, null, validSettings(), null);
        assertNull(tenant.getDeletedAt());
        tenant.delete();
        assertNotNull(tenant.getDeletedAt());
    }

    /* == pullDomainEvents == */

    @Test
    void pullDomainEvents_returnsEventsAndClearsQueue() {
        var tenant = Tenant.createTrial("acme", 1, null, validSettings(), null);
        tenant.activate();
        assertEquals(1, tenant.getDomainEvents().size());

        var events = tenant.pullDomainEvents();
        assertEquals(1, events.size());
        assertTrue(tenant.getDomainEvents().isEmpty());
    }

    /* == getDomainEvents == */

    @Test
    void getDomainEvents_returnsUnmodifiableList() {
        var tenant = Tenant.createTrial("acme", 1, null, validSettings(), null);
        assertThrows(UnsupportedOperationException.class,
                () -> tenant.getDomainEvents().add(null));
    }

    /* == restore == */

    @Test
    void restore_setsAllFields() {
        var now = LocalDateTime.now();
        var settings = validSettings();
        var tenant = Tenant.restore("id-1", 5, "acme", 2, settings, null, now, null, now, now, null);

        assertEquals("id-1", tenant.getId());
        assertEquals(5, tenant.getHoldingId());
        assertEquals("acme", tenant.getSubDomain());
        assertEquals(2, tenant.getPlanId());
        assertSame(settings, tenant.getSettings());
        assertEquals(now, tenant.getActivatedAt());
        assertTrue(tenant.isActivated());
        assertNull(tenant.getSuspendedAt());
        assertFalse(tenant.isSuspended());
    }

    /* == suspend == */

    @Test
    void suspend_setsSuspendedAt() {
        var tenant = Tenant.createTrial("acme", 1, null, validSettings(), null);
        tenant.activate();

        var before = LocalDateTime.now();
        tenant.suspend();
        var after = LocalDateTime.now();

        assertTrue(tenant.isSuspended());
        assertNotNull(tenant.getSuspendedAt());
        assertFalse(tenant.getSuspendedAt().isBefore(before));
        assertFalse(tenant.getSuspendedAt().isAfter(after));
    }

    @Test
    void suspend_throwsWhenNotActivated() {
        var tenant = Tenant.createPending("beta", 2, null);
        assertThrows(InvalidTenantTransitionException.class, tenant::suspend);
    }

    @Test
    void suspend_throwsWhenAlreadySuspended() {
        var tenant = Tenant.createTrial("acme", 1, null, validSettings(), null);
        tenant.activate();
        tenant.suspend();
        assertThrows(InvalidTenantTransitionException.class, tenant::suspend);
    }

    /* == reactivate == */

    @Test
    void reactivate_clearsSuspendedAt() {
        var tenant = Tenant.createTrial("acme", 1, null, validSettings(), null);
        tenant.activate();
        tenant.suspend();

        tenant.reactivate();

        assertFalse(tenant.isSuspended());
        assertNull(tenant.getSuspendedAt());
    }

    @Test
    void reactivate_throwsWhenNotSuspended() {
        var tenant = Tenant.createTrial("acme", 1, null, validSettings(), null);
        tenant.activate();
        assertThrows(InvalidTenantTransitionException.class, tenant::reactivate);
    }
}
