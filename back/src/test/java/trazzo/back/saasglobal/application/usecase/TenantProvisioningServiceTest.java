package trazzo.back.saasglobal.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import trazzo.back.saasglobal.application.dto.command.ActivateSubscriptionCommand;
import trazzo.back.saasglobal.application.dto.command.CreateTrialTenantCommand;
import trazzo.back.saasglobal.application.port.out.SubscriptionRepositoryPort;
import trazzo.back.saasglobal.application.port.out.TenantRepositoryPort;
import trazzo.back.saasglobal.application.port.out.TenantSchemaProvisioningPort;
import trazzo.back.saasglobal.domain.model.multitenancy.Subscription;
import trazzo.back.saasglobal.domain.model.multitenancy.SubscriptionStatus;
import trazzo.back.saasglobal.domain.model.multitenancy.Tenant;
import trazzo.back.saasglobal.domain.model.multitenancy.TenantSettings;

@ExtendWith(MockitoExtension.class)
class TenantProvisioningServiceTest {

    @Mock TenantRepositoryPort tenantRepository;
    @Mock SubscriptionRepositoryPort subscriptionRepository;
    @Mock TenantSchemaProvisioningPort schemaProvisioning;

    @InjectMocks TenantProvisioningService service;

    private static CreateTrialTenantCommand trialCmd() {
        return new CreateTrialTenantCommand(
                "acme", 1, 10,
                null, null, null, null
        );
    }

    private static TenantSettings someSettings() {
        return TenantSettings.of("t-1", "tenant_acme");
    }

    /* == createTrial == */

    @Test
    void createTrial_provisionsSchemaThenActivatesThenPersists() {
        when(tenantRepository.existsBySubDomain("acme")).thenReturn(false);
        when(tenantRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.createTrial(trialCmd());

        verify(schemaProvisioning).provisionExisting(any(TenantSettings.class));
        verify(tenantRepository).save(any(Tenant.class));
        verify(subscriptionRepository).save(any(Subscription.class));
        assertEquals("acme", result.subDomain());
        assertTrue(result.activated());
    }

    @Test
    void createTrial_deprovisionsSchemaWhenPersistenceFailsAfterProvisioning() {
        // Schema provisioning uses a raw, non-Spring-managed connection, so a failure after
        // it succeeded is not rolled back by @Transactional — the schema must be dropped
        // explicitly, or the orphaned schema would block retrying the same subDomain.
        when(tenantRepository.existsBySubDomain("acme")).thenReturn(false);
        when(tenantRepository.save(any())).thenThrow(new RuntimeException("db down"));

        assertThrows(RuntimeException.class, () -> service.createTrial(trialCmd()));

        verify(schemaProvisioning).provisionExisting(any(TenantSettings.class));
        verify(schemaProvisioning).deprovision("tenant_acme");
        verifyNoInteractions(subscriptionRepository);
    }

    @Test
    void createTrial_throwsWhenSubDomainAlreadyInUse() {
        when(tenantRepository.existsBySubDomain("acme")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.createTrial(trialCmd()));
        verifyNoInteractions(schemaProvisioning, subscriptionRepository);
    }

    @Test
    void createTrial_savesSubscriptionWithTrialStatusAndZeroPrice() {
        when(tenantRepository.existsBySubDomain(any())).thenReturn(false);
        when(tenantRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var subCaptor = ArgumentCaptor.forClass(Subscription.class);
        when(subscriptionRepository.save(subCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        service.createTrial(trialCmd());

        var savedSub = subCaptor.getValue();
        assertEquals(SubscriptionStatus.TRIAL, savedSub.getStatus());
        assertEquals(BigDecimal.ZERO, savedSub.getPurchasePrice());
    }

    /* == activate == */

    @Test
    void activate_activatesSubscriptionAndProvisionsThenActivatesTenant() {
        var tenantId = "t-1";
        var subId = "sub-1";
        var dateEnd = LocalDate.now().plusMonths(1);
        var sub = Subscription.restore(subId, 1, tenantId,
                LocalDate.now(), null, SubscriptionStatus.TRIAL, BigDecimal.ZERO, null, LocalDateTime.now());
        var tenant = Tenant.createPending("acme", 1, null);
        var settings = someSettings();

        when(subscriptionRepository.findById(subId)).thenReturn(Optional.of(sub));
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(schemaProvisioning.provisionNew(any(), any())).thenReturn(settings);
        when(tenantRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.activate(new ActivateSubscriptionCommand(subId, dateEnd));

        assertEquals(SubscriptionStatus.ACTIVE, sub.getStatus());
        assertTrue(result.activated());
        verify(schemaProvisioning).provisionNew(any(), eq("acme"));
        verify(tenantRepository).save(any(Tenant.class));
    }

    @Test
    void activate_throwsWhenSubscriptionNotFound() {
        when(subscriptionRepository.findById(any())).thenReturn(Optional.empty());
        var cmd = new ActivateSubscriptionCommand("missing", LocalDate.now());
        assertThrows(IllegalArgumentException.class, () -> service.activate(cmd));
    }

    @Test
    void activate_throwsWhenTenantNotFound() {
        var sub = Subscription.restore("sub-1", 1, "t-1",
                LocalDate.now(), null, SubscriptionStatus.TRIAL, BigDecimal.ZERO, null, LocalDateTime.now());
        when(subscriptionRepository.findById(any())).thenReturn(Optional.of(sub));
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tenantRepository.findById(any())).thenReturn(Optional.empty());
        var cmd = new ActivateSubscriptionCommand("sub-1", LocalDate.now());
        assertThrows(IllegalArgumentException.class, () -> service.activate(cmd));
    }
}
