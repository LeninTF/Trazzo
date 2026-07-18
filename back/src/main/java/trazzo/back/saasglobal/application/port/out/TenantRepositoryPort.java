package trazzo.back.saasglobal.application.port.out;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import trazzo.back.saasglobal.domain.model.multitenancy.Tenant;

public interface TenantRepositoryPort {
    Tenant save(Tenant tenant);
    Optional<Tenant> findById(String id);
    Optional<Tenant> findBySubDomain(String subDomain);
    boolean existsBySubDomain(String subDomain);

    /**
     * Hard delete, distinct from {@link Tenant#delete()}'s soft delete — used only to fully
     * undo a trial tenant whose checkout failed before any payment was ever attempted, so a
     * retry with the same data doesn't collide with leftover rows. Caller must delete
     * dependent subscriptions/users first (tenants is referenced ON DELETE RESTRICT).
     */
    void purgeById(String id);

    /**
     * Tenants created before {@code cutoff} whose subscription never left TRIAL status (Mercado
     * Pago never confirmed a payment) and that never had an ACTIVE/SUSPENDED one either — i.e.
     * never genuinely paid, ever. {@code tenants.activated_at} is set unconditionally by
     * TenantProvisioningService as soon as the schema is provisioned, so it does NOT indicate a
     * confirmed payment and must not be used as that signal here. Not soft-deleted — candidates
     * for {@link #purgeById}.
     */
    List<Tenant> findAbandonedTrials(LocalDateTime cutoff);

    // status filter: "TRIAL" | "ACTIVE" | "SUSPENDED" (derived from activated_at/suspended_at, not a column)
    List<Tenant> findAll(String search, Integer planId, String status, int page, int size);
    long countAll(String search, Integer planId, String status);

    // Metrics: each a single primitive count so SaasTenantService can compose the dashboard
    // formulas (growth %, churn %, variance) without the port dictating the business math.
    long countTotal();
    long countActive();
    long countCreatedSince(LocalDateTime since);
    long countTotalBefore(LocalDateTime cutoff);
    long countExistedBefore(LocalDateTime cutoff);
    long countDeletedBetween(LocalDateTime from, LocalDateTime toExclusive);
}
