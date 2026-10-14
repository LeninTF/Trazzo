package trazzo.back.saasglobal.infrastructure.adapters.out.provisioning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;

class TenantMigrationsHealthIndicatorTest {

    private final TenantSchemaMigrator migrator = mock(TenantSchemaMigrator.class);
    private final TenantMigrationsHealthIndicator indicator = new TenantMigrationsHealthIndicator(migrator);

    @Test
    void isUpWhenNoTenantFailed() {
        when(migrator.failedTenants()).thenReturn(0);

        assertEquals(Status.UP, indicator.health().getStatus());
    }

    @Test
    void isOutOfServiceWithoutDetailsWhenATenantFailed() {
        when(migrator.failedTenants()).thenReturn(2);

        var health = indicator.health();

        assertEquals(Status.OUT_OF_SERVICE, health.getStatus());
        assertTrue(health.getDetails().isEmpty());
    }
}
