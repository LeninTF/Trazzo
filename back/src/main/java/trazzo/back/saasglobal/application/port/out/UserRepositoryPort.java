package trazzo.back.saasglobal.application.port.out;

import java.util.List;
import java.util.Optional;
import trazzo.back.saasglobal.domain.model.iam.User;

public interface UserRepositoryPort {
    Optional<User> findByEmail(String email);
    Optional<User> findById(String id);
    Optional<User> findByTenantId(String tenantId);

    /**
     * Every user row for a tenant, including soft-deleted ones — unlike {@link #findByTenantId},
     * which only returns the first non-deleted match. Used for hard-delete compensation
     * (rolling back a failed checkout), where missing a row would leave it behind holding a
     * {@code users.tenant_id} FK and blocking the tenant's purge.
     */
    List<User> findAllByTenantId(String tenantId);

    List<User> findAll(String search, int page, int size);
    long countAll(String search);
    User save(User user);
    User update(User user);
}
