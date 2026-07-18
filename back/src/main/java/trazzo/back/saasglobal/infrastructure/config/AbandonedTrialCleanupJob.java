package trazzo.back.saasglobal.infrastructure.config;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import trazzo.back.saasglobal.application.port.out.TenantRepositoryPort;
import trazzo.back.saasglobal.application.usecase.TenantPurgeService;
import trazzo.back.saasglobal.domain.model.multitenancy.Tenant;

/**
 * A /shop self-signup tenant's subscription is created as TRIAL immediately and only moves to
 * ACTIVE once Mercado Pago confirms the payer authorized the charge (the tenant row itself
 * activates as soon as its schema is provisioned, well before any payment — see
 * TenantRepositoryPort#findAbandonedTrials). If the payer never completes checkout on Mercado
 * Pago's page, that tenant (and its admin user/person) would otherwise sit in TRIAL forever,
 * permanently occupying its document/email and blocking a retry with the same data. This sweeps
 * and purges those abandoned trials periodically.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AbandonedTrialCleanupJob {

    private final TenantRepositoryPort tenantRepository;
    private final TenantPurgeService tenantPurgeService;

    @Value("${trazzo.shop.abandoned-trial-hours:24}")
    private int abandonedTrialHours;

    @Scheduled(cron = "${trazzo.shop.abandoned-trial-cleanup-cron:0 0 * * * *}")
    public void purgeAbandonedTrials() {
        LocalDateTime cutoff = LocalDateTime.now(Clock.systemDefaultZone()).minusHours(abandonedTrialHours);
        List<Tenant> abandoned = tenantRepository.findAbandonedTrials(cutoff);
        if (abandoned.isEmpty()) {
            return;
        }
        log.info("Purging {} abandoned trial tenant(s) created before {}", abandoned.size(), cutoff);
        abandoned.forEach(tenant -> tenantPurgeService.purge(tenant.getId()));
    }
}
