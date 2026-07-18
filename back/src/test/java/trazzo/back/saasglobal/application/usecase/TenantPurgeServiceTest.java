package trazzo.back.saasglobal.application.usecase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import trazzo.back.saasglobal.application.port.out.PersonRepositoryPort;
import trazzo.back.saasglobal.application.port.out.SubscriptionRepositoryPort;
import trazzo.back.saasglobal.application.port.out.TenantRepositoryPort;
import trazzo.back.saasglobal.application.port.out.TenantSchemaProvisioningPort;
import trazzo.back.saasglobal.application.port.out.UserRepositoryPort;
import trazzo.back.saasglobal.domain.model.iam.User;
import trazzo.back.saasglobal.domain.model.multitenancy.Tenant;
import trazzo.back.saasglobal.domain.model.multitenancy.TenantSettings;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TenantPurgeServiceTest {

    @Mock TenantRepositoryPort tenantRepository;
    @Mock TenantSchemaProvisioningPort schemaProvisioning;
    @Mock SubscriptionRepositoryPort subscriptionRepository;
    @Mock PersonRepositoryPort personRepository;
    @Mock UserRepositoryPort userRepository;
    @InjectMocks TenantPurgeService service;

    private static Tenant tenantWithSchema() {
        return Tenant.restore("tenant-1", null, "acme-sac", 2,
                TenantSettings.of("tenant-1", "tenant_acme_sac"), null, LocalDateTime.now(), null,
                LocalDateTime.now(), LocalDateTime.now(), null);
    }

    @Test
    void purge_deletesUserPersonSubscriptionTenantAndSchema() {
        when(tenantRepository.findById("tenant-1")).thenReturn(Optional.of(tenantWithSchema()));
        User user = User.create(1, "tenant-1", "juan@acme.pe", "999999999", "encoded", true);
        when(userRepository.findAllByTenantId("tenant-1")).thenReturn(List.of(user));

        service.purge("tenant-1");

        verify(personRepository).deleteById(1);
        verify(subscriptionRepository).deleteByTenantId("tenant-1");
        verify(tenantRepository).purgeById("tenant-1");
        verify(schemaProvisioning).deprovision("tenant_acme_sac");
    }

    @Test
    void purge_deletesEveryUserForTheTenant_notJustTheFirstOne() {
        // Guards against reusing findByTenantId (first-non-deleted-only): any row it would
        // miss (a second user, or a soft-deleted one) keeps holding the users.tenant_id FK
        // and silently blocks tenantRepository.purgeById.
        when(tenantRepository.findById("tenant-1")).thenReturn(Optional.of(tenantWithSchema()));
        User activeUser = User.create(1, "tenant-1", "juan@acme.pe", "999999999", "encoded", true);
        User softDeletedUser = User.restore("u-2", 2, "tenant-1", "old@acme.pe", null, "encoded",
                List.of(), List.of(), true, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());
        when(userRepository.findAllByTenantId("tenant-1")).thenReturn(List.of(activeUser, softDeletedUser));

        service.purge("tenant-1");

        verify(personRepository).deleteById(1);
        verify(personRepository).deleteById(2);
        verify(tenantRepository).purgeById("tenant-1");
    }

    @Test
    void purge_skipsPersonDeletionWhenNoUsersExist() {
        when(tenantRepository.findById("tenant-1")).thenReturn(Optional.of(tenantWithSchema()));
        when(userRepository.findAllByTenantId("tenant-1")).thenReturn(List.of());

        service.purge("tenant-1");

        verify(personRepository, never()).deleteById(any());
        verify(subscriptionRepository).deleteByTenantId("tenant-1");
        verify(tenantRepository).purgeById("tenant-1");
        verify(schemaProvisioning).deprovision("tenant_acme_sac");
    }

    @Test
    void purge_skipsDeprovisionWhenTenantHasNoSchema() {
        when(tenantRepository.findById("tenant-1")).thenReturn(Optional.empty());
        when(userRepository.findAllByTenantId("tenant-1")).thenReturn(List.of());

        service.purge("tenant-1");

        verify(tenantRepository).purgeById("tenant-1");
        verify(schemaProvisioning, never()).deprovision(any());
    }

    @Test
    void purge_swallowsFailuresBestEffort() {
        when(tenantRepository.findById("tenant-1")).thenThrow(new RuntimeException("db down"));

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> service.purge("tenant-1"));
    }
}
