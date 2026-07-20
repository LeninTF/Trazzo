package trazzo.back.saasglobal.application.usecase;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import trazzo.back.saasglobal.application.dto.result.PaginatedResult;
import trazzo.back.saasglobal.application.dto.result.TenantMetricsResult;
import trazzo.back.saasglobal.application.dto.result.TenantResult;
import trazzo.back.saasglobal.application.port.in.SaasTenantUseCase;
import trazzo.back.saasglobal.application.port.out.HoldingRepositoryPort;
import trazzo.back.saasglobal.application.port.out.PlanRepositoryPort;
import trazzo.back.saasglobal.application.port.out.TenantRepositoryPort;
import trazzo.back.saasglobal.domain.model.multitenancy.Holding;
import trazzo.back.saasglobal.domain.model.multitenancy.Plan;
import trazzo.back.saasglobal.domain.model.multitenancy.Tenant;
import trazzo.back.saasglobal.domain.model.multitenancy.TenantBranding;

@Service
@RequiredArgsConstructor
public class SaasTenantService implements SaasTenantUseCase {

    // Business goal, not a measured value — same pattern as RequestService.MAX_REQUESTS_PER_TAX_ID.
    private static final int NUEVOS_TENANTS_GOAL = 30;

    private final TenantRepositoryPort tenantRepository;
    private final HoldingRepositoryPort holdingRepository;
    private final PlanRepositoryPort planRepository;

    @Override
    public PaginatedResult<TenantResult> listAll(String search, Integer planId, String status, int page, int size) {
        List<TenantResult> results = tenantRepository.findAll(search, planId, status, page, size)
                .stream().map(this::toResult).toList();
        long total = tenantRepository.countAll(search, planId, status);
        return PaginatedResult.of(results, page, size, total);
    }

    @Override
    public TenantResult getById(String id) {
        return toResult(findOrThrow(id));
    }

    @Override
    public TenantMetricsResult getMetrics() {
        LocalDateTime now = LocalDateTime.now(Clock.systemDefaultZone());
        LocalDateTime cutoff30 = now.minusDays(30);
        LocalDateTime cutoff60 = now.minusDays(60);

        long total = tenantRepository.countTotal();
        long totalBefore30d = tenantRepository.countTotalBefore(cutoff30);
        double crecimientoPct = percentage(total - totalBefore30d, totalBefore30d);

        long activos = tenantRepository.countActive();
        double porcentajeActivos = percentage(activos, total);

        long nuevos30d = tenantRepository.countCreatedSince(cutoff30);

        long existedBefore30d = tenantRepository.countExistedBefore(cutoff30);
        long deletedLast30d = tenantRepository.countDeletedBetween(cutoff30, null);
        double churnActual = percentage(deletedLast30d, existedBefore30d);

        long existedBefore60d = tenantRepository.countExistedBefore(cutoff60);
        long deletedPrior30to60d = tenantRepository.countDeletedBetween(cutoff60, cutoff30);
        double churnPrevious = percentage(deletedPrior30to60d, existedBefore60d);

        return new TenantMetricsResult(total, crecimientoPct, activos, porcentajeActivos,
                nuevos30d, NUEVOS_TENANTS_GOAL, churnActual, churnActual - churnPrevious);
    }

    @Override
    public TenantResult suspend(String id) {
        Tenant tenant = findOrThrow(id);
        tenant.suspend();
        return toResult(tenantRepository.save(tenant));
    }

    @Override
    public TenantResult reactivate(String id) {
        Tenant tenant = findOrThrow(id);
        tenant.reactivate();
        return toResult(tenantRepository.save(tenant));
    }

    @Override
    public TenantResult updateBranding(String id, String logoUrl, String slogan, String primaryColor, String secondaryColor) {
        Tenant tenant = findOrThrow(id);
        tenant.assignBranding(TenantBranding.of(id, logoUrl, slogan, primaryColor, secondaryColor));
        return toResult(tenantRepository.save(tenant));
    }

    @Override
    public void deleteById(String id) {
        Tenant tenant = findOrThrow(id);
        tenant.delete();
        tenantRepository.save(tenant);
    }

    private Tenant findOrThrow(String id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + id));
    }

    private TenantResult toResult(Tenant tenant) {
        String holdingName = tenant.getHoldingId() != null
                ? holdingRepository.findById(tenant.getHoldingId()).map(Holding::getLegalName).orElse(null)
                : null;
        String planName = planRepository.findById(tenant.getPlanId()).map(Plan::getName).orElse(null);
        return new TenantResult(tenant.getId(), tenant.getSubDomain(), tenant.getHoldingId(), holdingName,
                tenant.getPlanId(), planName, statusOf(tenant), tenant.getActivatedAt(), tenant.getCreatedAt());
    }

    private static String statusOf(Tenant tenant) {
        if (tenant.isSuspended()) {
            return "SUSPENDED";
        }
        return tenant.isActivated() ? "ACTIVE" : "TRIAL";
    }

    private static double percentage(long numerator, long denominator) {
        return denominator > 0 ? (numerator * 100.0) / denominator : 0.0;
    }
}
