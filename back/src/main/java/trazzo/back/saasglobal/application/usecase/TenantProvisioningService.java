package trazzo.back.saasglobal.application.usecase;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import trazzo.back.saasglobal.application.dto.command.ActivateSubscriptionCommand;
import trazzo.back.saasglobal.application.dto.command.CreateTrialTenantCommand;
import trazzo.back.saasglobal.application.dto.result.TenantResultDto;
import trazzo.back.saasglobal.application.port.in.ActivateSubscriptionUseCase;
import trazzo.back.saasglobal.application.port.in.CreateTrialTenantUseCase;
import trazzo.back.saasglobal.application.port.out.SubscriptionRepositoryPort;
import trazzo.back.saasglobal.application.port.out.TenantRepositoryPort;
import trazzo.back.saasglobal.application.port.out.TenantSchemaProvisioningPort;
import trazzo.back.saasglobal.domain.model.multitenancy.Subscription;
import trazzo.back.saasglobal.domain.model.multitenancy.Tenant;
import trazzo.back.saasglobal.domain.model.multitenancy.TenantBranding;
import trazzo.back.saasglobal.domain.model.multitenancy.TenantSettings;

@Service
@RequiredArgsConstructor
public class TenantProvisioningService implements CreateTrialTenantUseCase, ActivateSubscriptionUseCase {

    private final TenantRepositoryPort tenantRepository;
    private final SubscriptionRepositoryPort subscriptionRepository;
    private final TenantSchemaProvisioningPort schemaProvisioning;

    /**
     * TRIAL flow (manual): admin provides existing DB credentials.
     * 1. Build Tenant with provided settings
     * 2. Run schema script against that DB
     * 3. Activate tenant + persist
     * 4. Create TRIAL subscription
     */
    @Override
    @Transactional
    public TenantResultDto createTrial(CreateTrialTenantCommand cmd) {
        validateSubDomainFormat(cmd.subDomain());
        validateSubDomainUnique(cmd.subDomain());

        Tenant tenant = Tenant.createTrial(
                cmd.subDomain(),
                cmd.planId(),
                cmd.holdingId(),
                buildSettings(cmd),
                buildBranding(cmd)
        );

        schemaProvisioning.provisionExisting(tenant.getSettings());

        tenant.activate();
        Tenant saved = tenantRepository.save(tenant);

        Subscription subscription = Subscription.createTrial(
                saved.getId(), saved.getPlanId(), BigDecimal.ZERO, LocalDate.now(Clock.systemDefaultZone()));
        subscriptionRepository.save(subscription);

        return toResult(saved);
    }

    /**
     * PAID flow (automatic): triggered after payment confirmation.
     * 1. Load subscription and activate it
     * 2. Load tenant
     * 3. Create new isolated DB + run schema
     * 4. Assign settings to tenant + activate + persist
     * If master DB operations fail after provisioning, deprovision() is called as compensation.
     */
    @Override
    @Transactional
    public TenantResultDto activate(ActivateSubscriptionCommand cmd) {
        Subscription subscription = subscriptionRepository.findById(cmd.subscriptionId())
                .orElseThrow(() -> new IllegalArgumentException("subscription not found: " + cmd.subscriptionId()));

        subscription.activate(cmd.dateEnd());
        subscriptionRepository.save(subscription);

        Tenant tenant = tenantRepository.findById(subscription.getTenantId())
                .orElseThrow(() -> new IllegalArgumentException("tenant not found: " + subscription.getTenantId()));

        TenantSettings settings = schemaProvisioning.provisionNew(tenant.getId(), tenant.getSubDomain());
        try {
            tenant.assignSettings(settings);
            tenant.activate();
            return toResult(tenantRepository.save(tenant));
        } catch (Exception e) {
            schemaProvisioning.deprovision(settings.getSchemaName());
            throw e;
        }
    }

    private void validateSubDomainFormat(String subDomain) {
        if (subDomain == null || !subDomain.matches("[a-z0-9][a-z0-9\\-]*[a-z0-9]|[a-z0-9]")) {
            throw new IllegalArgumentException(
                    "subDomain must use only lowercase letters, digits, and hyphens: " + subDomain);
        }
    }

    private void validateSubDomainUnique(String subDomain) {
        if (tenantRepository.existsBySubDomain(subDomain)) {
            throw new IllegalArgumentException("subDomain already in use: " + subDomain);
        }
    }

    private TenantSettings buildSettings(CreateTrialTenantCommand cmd) {
        return TenantSettings.of(null, TenantSettings.deriveSchemaName(cmd.subDomain()));
    }

    private TenantBranding buildBranding(CreateTrialTenantCommand cmd) {
        if (cmd.logoUrl() == null && cmd.slogan() == null
                && cmd.primaryColor() == null && cmd.secondaryColor() == null) {
            return null;
        }
        return TenantBranding.of(null, cmd.logoUrl(), cmd.slogan(), cmd.primaryColor(), cmd.secondaryColor());
    }

    private TenantResultDto toResult(Tenant tenant) {
        return new TenantResultDto(
                tenant.getId(),
                tenant.getSubDomain(),
                tenant.getPlanId(),
                tenant.isActivated(),
                tenant.getActivatedAt(),
                tenant.getCreatedAt()
        );
    }
}
