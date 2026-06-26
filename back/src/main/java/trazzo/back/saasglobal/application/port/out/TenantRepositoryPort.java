package trazzo.back.saasglobal.application.port.out;

import java.util.Optional;
import trazzo.back.saasglobal.domain.model.multitenancy.Tenant;

public interface TenantRepositoryPort {
    Tenant save(Tenant tenant);
    Optional<Tenant> findById(String id);
    Optional<Tenant> findBySubDomain(String subDomain);
    boolean existsBySubDomain(String subDomain);
}
