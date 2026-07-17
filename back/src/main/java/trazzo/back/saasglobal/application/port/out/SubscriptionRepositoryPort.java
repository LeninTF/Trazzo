package trazzo.back.saasglobal.application.port.out;

import java.util.List;
import java.util.Optional;
import trazzo.back.saasglobal.domain.model.multitenancy.Subscription;

public interface SubscriptionRepositoryPort {
    Subscription save(Subscription subscription);
    Optional<Subscription> findById(String id);
    Optional<Subscription> findActiveByTenantId(String tenantId);
    Optional<Subscription> findByMpPreapprovalId(String mpPreapprovalId);
    List<Subscription> findAll(int page, int size);
    long countAll();

    /** Hard delete — used only to compensate a checkout that failed before any payment attempt. */
    void deleteByTenantId(String tenantId);
}
