package trazzo.back.audit.application.port.out;

import java.util.Optional;

public interface TenantInfoPort {
    Optional<TenantInfo> findByTenantId(String tenantId);

    record TenantInfo(String tenantId, String tenantName) {}
}
