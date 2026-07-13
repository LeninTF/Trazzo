package trazzo.back.saasglobal.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import trazzo.back.saasglobal.application.dto.result.PaginatedResult;
import trazzo.back.saasglobal.application.dto.result.SubscriptionResult;
import trazzo.back.saasglobal.application.port.out.PlanRepositoryPort;
import trazzo.back.saasglobal.application.port.out.SubscriptionRepositoryPort;
import trazzo.back.saasglobal.application.port.out.TenantRepositoryPort;
import trazzo.back.saasglobal.domain.model.multitenancy.Plan;
import trazzo.back.saasglobal.domain.model.multitenancy.Subscription;
import trazzo.back.saasglobal.domain.model.multitenancy.SubscriptionStatus;
import trazzo.back.saasglobal.domain.model.multitenancy.Tenant;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock SubscriptionRepositoryPort subscriptionRepository;
    @Mock TenantRepositoryPort tenantRepository;
    @Mock PlanRepositoryPort planRepository;
    @InjectMocks SubscriptionService service;

    private static Subscription subscription() {
        return Subscription.restore("sub-1", 1, "tenant-1", LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31), SubscriptionStatus.ACTIVE, new BigDecimal("29.99"), LocalDateTime.now());
    }

    private static Tenant tenant() {
        return Tenant.restore("tenant-1", null, "demo", 1, null, null, null, null, LocalDateTime.now(), null, null);
    }

    private static Plan plan() {
        return Plan.restore(1, "Plan Demo", new BigDecimal("29.99"), null, "SOLES", "MONTHLY",
                true, LocalDateTime.now(), null, null);
    }

    @Test
    void listAll_resolvesTenantAndPlanNames() {
        when(subscriptionRepository.findAll(0, 20)).thenReturn(List.of(subscription()));
        when(subscriptionRepository.countAll()).thenReturn(1L);
        when(tenantRepository.findById("tenant-1")).thenReturn(Optional.of(tenant()));
        when(planRepository.findById(1)).thenReturn(Optional.of(plan()));

        PaginatedResult<SubscriptionResult> result = service.listAll(0, 20);

        assertEquals(1, result.content().size());
        SubscriptionResult first = result.content().get(0);
        assertEquals("sub-1", first.id());
        assertEquals("demo", first.tenantName());
        assertEquals("Plan Demo", first.planName());
        assertEquals("ACTIVE", first.status());
    }

    @Test
    void listAll_fallsBackToTenantIdWhenTenantMissing() {
        when(subscriptionRepository.findAll(0, 20)).thenReturn(List.of(subscription()));
        when(subscriptionRepository.countAll()).thenReturn(1L);
        when(tenantRepository.findById("tenant-1")).thenReturn(Optional.empty());
        when(planRepository.findById(1)).thenReturn(Optional.empty());

        PaginatedResult<SubscriptionResult> result = service.listAll(0, 20);

        SubscriptionResult first = result.content().get(0);
        assertEquals("tenant-1", first.tenantName());
        assertNull(first.planName());
    }

    @Test
    void listAll_returnsEmptyWhenNoSubscriptions() {
        when(subscriptionRepository.findAll(0, 20)).thenReturn(List.of());
        when(subscriptionRepository.countAll()).thenReturn(0L);

        PaginatedResult<SubscriptionResult> result = service.listAll(0, 20);

        assertTrue(result.content().isEmpty());
        assertEquals(0, result.totalElements());
    }
}
