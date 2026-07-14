package trazzo.back.audit.application.port.out;

import java.util.Optional;

public interface TenantInfoPort {
    Optional<TenantInfo> findByUserId(String userId);

    record TenantInfo(String tenantId, String tenantName) {}
}
