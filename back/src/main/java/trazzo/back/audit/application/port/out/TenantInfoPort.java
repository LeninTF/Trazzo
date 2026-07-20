package trazzo.back.audit.application.port.out;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface TenantInfoPort {
    Optional<TenantInfo> findByUserId(String userId);

    Map<String, TenantInfo> findByUserIds(List<String> userIds);

    record TenantInfo(String tenantId, String tenantName) {}
}
