package trazzo.back.saasglobal.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import trazzo.back.saasglobal.application.dto.result.PaginatedResult;
import trazzo.back.saasglobal.application.dto.result.TenantMetricsResult;
import trazzo.back.saasglobal.application.dto.result.TenantResult;
import trazzo.back.saasglobal.application.port.out.HoldingRepositoryPort;
import trazzo.back.saasglobal.application.port.out.PlanRepositoryPort;
import trazzo.back.saasglobal.application.port.out.TenantRepositoryPort;
import trazzo.back.saasglobal.domain.exception.InvalidTenantTransitionException;
import trazzo.back.saasglobal.domain.model.multitenancy.Holding;
import trazzo.back.saasglobal.domain.model.multitenancy.HoldingType;
import trazzo.back.saasglobal.domain.model.multitenancy.Plan;
import trazzo.back.saasglobal.domain.model.multitenancy.Tenant;
import trazzo.back.saasglobal.domain.model.multitenancy.TenantSettings;

@ExtendWith(MockitoExtension.class)
class SaasTenantServiceTest {

    @Mock TenantRepositoryPort tenantRepository;
    @Mock HoldingRepositoryPort holdingRepository;
    @Mock PlanRepositoryPort planRepository;
    @InjectMocks SaasTenantService service;

    private static Tenant activeTenant() {
        var tenant = Tenant.createTrial("acme", 1, 10, TenantSettings.of(null, "tenant_acme"), null);
        tenant.activate();
        return tenant;
    }

    private static Holding holding() {
        return Holding.restore(10, "20111111111", "Acme SAC", HoldingType.PRIVADO, true, LocalDateTime.now(), LocalDateTime.now(), null);
    }

    private static Plan plan() {
        return Plan.restore(1, "Plan Demo", new BigDecimal("29.99"), null, "SOLES", "MONTHLY",
                true, LocalDateTime.now(), null, null);
    }

    @Test
    void listAll_resolvesHoldingAndPlanNames() {
        var tenant = activeTenant();
        when(tenantRepository.findAll(any(), any(), any(), anyInt(), anyInt())).thenReturn(List.of(tenant));
        when(tenantRepository.countAll(any(), any(), any())).thenReturn(1L);
        when(holdingRepository.findById(10)).thenReturn(Optional.of(holding()));
        when(planRepository.findById(1)).thenReturn(Optional.of(plan()));

        PaginatedResult<TenantResult> result = service.listAll(null, null, null, 0, 20);

        assertEquals(1, result.content().size());
        TenantResult first = result.content().get(0);
        assertEquals("Acme SAC", first.holdingName());
        assertEquals("Plan Demo", first.planName());
        assertEquals("ACTIVE", first.status());
    }

    @Test
    void listAll_fallsBackToNullNamesWhenNotFound() {
        var tenant = activeTenant();
        when(tenantRepository.findAll(any(), any(), any(), anyInt(), anyInt())).thenReturn(List.of(tenant));
        when(tenantRepository.countAll(any(), any(), any())).thenReturn(1L);
        when(holdingRepository.findById(10)).thenReturn(Optional.empty());
        when(planRepository.findById(1)).thenReturn(Optional.empty());

        TenantResult first = service.listAll(null, null, null, 0, 20).content().get(0);

        assertNull(first.holdingName());
        assertNull(first.planName());
    }

    @Test
    void getById_throwsWhenNotFound() {
        when(tenantRepository.findById("missing")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.getById("missing"));
    }

    @Test
    void suspend_transitionsAndSaves() {
        var tenant = activeTenant();
        when(tenantRepository.findById(tenant.getId())).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(holdingRepository.findById(10)).thenReturn(Optional.of(holding()));
        when(planRepository.findById(1)).thenReturn(Optional.of(plan()));

        TenantResult result = service.suspend(tenant.getId());

        assertEquals("SUSPENDED", result.status());
    }

    @Test
    void suspend_throwsWhenNotActivated() {
        var tenant = Tenant.createPending("beta", 2, null);
        when(tenantRepository.findById(tenant.getId())).thenReturn(Optional.of(tenant));

        assertThrows(InvalidTenantTransitionException.class, () -> service.suspend(tenant.getId()));
    }

    @Test
    void reactivate_transitionsAndSaves() {
        var tenant = activeTenant();
        tenant.suspend();
        when(tenantRepository.findById(tenant.getId())).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(holdingRepository.findById(10)).thenReturn(Optional.of(holding()));
        when(planRepository.findById(1)).thenReturn(Optional.of(plan()));

        TenantResult result = service.reactivate(tenant.getId());

        assertEquals("ACTIVE", result.status());
    }

    @Test
    void updateBranding_assignsAndSaves() {
        var tenant = activeTenant();
        when(tenantRepository.findById(tenant.getId())).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(holdingRepository.findById(10)).thenReturn(Optional.of(holding()));
        when(planRepository.findById(1)).thenReturn(Optional.of(plan()));

        service.updateBranding(tenant.getId(), "http://logo", "slogan", "#111", "#222");

        assertNotNull(tenant.getBranding());
        assertEquals("http://logo", tenant.getBranding().getLogoUrl());
    }

    @Test
    void deleteById_marksTenantDeleted() {
        var tenant = activeTenant();
        when(tenantRepository.findById(tenant.getId())).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.deleteById(tenant.getId());

        assertNotNull(tenant.getDeletedAt());
    }

    @Test
    void deleteById_throwsWhenNotFound() {
        when(tenantRepository.findById("missing")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.deleteById("missing"));
    }

    @Test
    void getMetrics_computesFromCounts() {
        when(tenantRepository.countTotal()).thenReturn(110L);
        when(tenantRepository.countTotalBefore(any())).thenReturn(100L);
        when(tenantRepository.countActive()).thenReturn(90L);
        when(tenantRepository.countCreatedSince(any())).thenReturn(15L);
        when(tenantRepository.countExistedBefore(any())).thenReturn(100L, 80L);
        when(tenantRepository.countDeletedBetween(any(), any())).thenReturn(5L, 4L);

        TenantMetricsResult metrics = service.getMetrics();

        assertEquals(110L, metrics.total());
        assertEquals(10.0, metrics.crecimientoPct(), 0.001);
        assertEquals(90L, metrics.activos());
        assertEquals(15L, metrics.nuevos30d());
        assertEquals(30, metrics.nuevosMeta());
        assertEquals(5.0, metrics.tasaChurnPct(), 0.001);
    }

    @Test
    void getMetrics_handlesZeroDenominatorsWithoutDivideByZero() {
        when(tenantRepository.countTotal()).thenReturn(0L);
        when(tenantRepository.countTotalBefore(any())).thenReturn(0L);
        when(tenantRepository.countActive()).thenReturn(0L);
        when(tenantRepository.countCreatedSince(any())).thenReturn(0L);
        when(tenantRepository.countExistedBefore(any())).thenReturn(0L);
        when(tenantRepository.countDeletedBetween(any(), any())).thenReturn(0L);

        TenantMetricsResult metrics = service.getMetrics();

        assertEquals(0.0, metrics.crecimientoPct());
        assertEquals(0.0, metrics.porcentajeActivos());
        assertEquals(0.0, metrics.tasaChurnPct());
    }
}
