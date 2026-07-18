package trazzo.back.saasglobal.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import trazzo.back.saasglobal.application.dto.command.SubscribeToPlanCommand;
import trazzo.back.saasglobal.application.dto.result.SubscribeToPlanResult;
import trazzo.back.saasglobal.application.port.out.AppConfigPort;
import trazzo.back.saasglobal.application.port.out.MercadoPagoSubscriptionPort;
import trazzo.back.saasglobal.application.port.out.MercadoPagoSubscriptionPort.PreapprovalCreated;
import trazzo.back.saasglobal.application.port.out.PlanRepositoryPort;
import trazzo.back.saasglobal.application.port.out.SubscriptionRepositoryPort;
import trazzo.back.saasglobal.application.port.out.TenantRepositoryPort;
import trazzo.back.saasglobal.domain.model.multitenancy.Plan;
import trazzo.back.saasglobal.domain.model.multitenancy.Subscription;
import trazzo.back.saasglobal.domain.model.multitenancy.SubscriptionStatus;
import trazzo.back.saasglobal.domain.model.multitenancy.Tenant;
import trazzo.back.saasglobal.domain.model.multitenancy.TenantSettings;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TenantPlanSubscriptionServiceTest {

    @Mock PlanRepositoryPort planRepository;
    @Mock TenantRepositoryPort tenantRepository;
    @Mock SubscriptionRepositoryPort subscriptionRepository;
    @Mock MercadoPagoSubscriptionPort mercadoPagoSubscriptionPort;
    @Mock AppConfigPort appConfig;
    @InjectMocks TenantPlanSubscriptionService service;

    private static Plan plan() {
        return Plan.restore(3, "Plan Pro", new BigDecimal("59.99"), null, "SOLES", "MONTHLY",
                true, LocalDateTime.now(), null, null);
    }

    private static Tenant tenant() {
        return Tenant.restore("tenant-1", null, "acme", 2,
                TenantSettings.of("tenant-1", "tenant_acme"), null, LocalDateTime.now(), null,
                LocalDateTime.now(), LocalDateTime.now(), null);
    }

    @Test
    void subscribe_cancelsCurrentSubscriptionAndCreatesNewOne() {
        when(appConfig.frontendUrl()).thenReturn("http://localhost:4200");
        when(planRepository.findById(3)).thenReturn(Optional.of(plan()));
        when(tenantRepository.findById("tenant-1")).thenReturn(Optional.of(tenant()));
        Subscription current = Subscription.createTrial("tenant-1", 2, BigDecimal.ZERO, LocalDate.now());
        current.activate(LocalDate.now().plusMonths(1));
        when(subscriptionRepository.findActiveByTenantIdForUpdate("tenant-1")).thenReturn(Optional.of(current));
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mercadoPagoSubscriptionPort.createPreapproval(any()))
                .thenReturn(new PreapprovalCreated("preapproval-2", "pending", "https://mp/init", "https://mp/sandbox-init"));

        SubscribeToPlanResult result = service.subscribe(new SubscribeToPlanCommand("tenant-1", "admin@acme.pe", 3));

        assertNotNull(result.subscriptionId());
        assertEquals("https://mp/sandbox-init", result.initPoint());
        assertEquals(SubscriptionStatus.CANCELED, current.getStatus());
        verify(subscriptionRepository, org.mockito.Mockito.atLeastOnce()).save(any());
    }

    @Test
    void subscribe_throwsWhenPlanNotFound() {
        when(planRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.subscribe(new SubscribeToPlanCommand("tenant-1", "admin@acme.pe", 99)));
    }

    @Test
    void subscribe_throwsWhenPlanInactive() {
        Plan inactive = Plan.restore(3, "Plan Pro", new BigDecimal("59.99"), null, "SOLES", "MONTHLY",
                false, LocalDateTime.now(), null, null);
        when(planRepository.findById(3)).thenReturn(Optional.of(inactive));

        assertThrows(IllegalArgumentException.class,
                () -> service.subscribe(new SubscribeToPlanCommand("tenant-1", "admin@acme.pe", 3)));
    }

    @Test
    void subscribe_throwsWhenTenantNotFound() {
        when(planRepository.findById(3)).thenReturn(Optional.of(plan()));
        when(tenantRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> service.subscribe(new SubscribeToPlanCommand("missing", "admin@acme.pe", 3)));
    }

    @Test
    void subscribe_doesNotCancelWhenNoCurrentSubscription() {
        when(appConfig.frontendUrl()).thenReturn("http://localhost:4200");
        when(planRepository.findById(3)).thenReturn(Optional.of(plan()));
        when(tenantRepository.findById("tenant-1")).thenReturn(Optional.of(tenant()));
        when(subscriptionRepository.findActiveByTenantIdForUpdate("tenant-1")).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mercadoPagoSubscriptionPort.createPreapproval(any()))
                .thenReturn(new PreapprovalCreated("preapproval-2", "pending", "https://mp/init", null));

        service.subscribe(new SubscribeToPlanCommand("tenant-1", "admin@acme.pe", 3));

        verify(subscriptionRepository, org.mockito.Mockito.times(2)).save(any());
        verify(subscriptionRepository, never()).findByMpPreapprovalId(any());
    }
}
