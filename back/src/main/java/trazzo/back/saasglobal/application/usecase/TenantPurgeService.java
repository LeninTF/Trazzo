package trazzo.back.saasglobal.application.usecase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import trazzo.back.saasglobal.application.port.out.PersonRepositoryPort;
import trazzo.back.saasglobal.application.port.out.SubscriptionRepositoryPort;
import trazzo.back.saasglobal.application.port.out.TenantRepositoryPort;
import trazzo.back.saasglobal.application.port.out.TenantSchemaProvisioningPort;
import trazzo.back.saasglobal.application.port.out.UserRepositoryPort;
import trazzo.back.saasglobal.domain.model.multitenancy.Tenant;
import trazzo.back.saasglobal.domain.model.multitenancy.TenantSettings;

/**
 * Hard-deletes a tenant that never completed activation: every user's person (cascades the
 * user), its subscriptions, the tenant row, and its schema. Shared by ShopCheckoutService (to
 * compensate a checkout that failed before Mercado Pago ever confirmed anything) and
 * AbandonedTrialCleanupJob (to sweep tenants whose payment was simply never confirmed).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantPurgeService {

    private final TenantRepositoryPort tenantRepository;
    private final TenantSchemaProvisioningPort schemaProvisioning;
    private final SubscriptionRepositoryPort subscriptionRepository;
    private final PersonRepositoryPort personRepository;
    private final UserRepositoryPort userRepository;

    public void purge(String tenantId) {
        try {
            String schemaName = tenantRepository.findById(tenantId)
                    .map(Tenant::getSettings)
                    .map(TenantSettings::getSchemaName)
                    .orElse(null);
            // findAllByTenantId (not findByTenantId): the latter filters out soft-deleted rows
            // and returns only the first match — either gap would leave a user row behind
            // holding the tenants.tenant_id FK and silently blocking purgeById below.
            userRepository.findAllByTenantId(tenantId)
                    .forEach(user -> personRepository.deleteById(user.getPersonId()));
            subscriptionRepository.deleteByTenantId(tenantId);
            tenantRepository.purgeById(tenantId);
            if (schemaName != null) {
                schemaProvisioning.deprovision(schemaName);
            }
            log.info("Purged tenant {}", tenantId);
        } catch (RuntimeException e) {
            log.error("Failed to fully purge tenant {} — manual cleanup needed", tenantId, e);
        }
    }
}
