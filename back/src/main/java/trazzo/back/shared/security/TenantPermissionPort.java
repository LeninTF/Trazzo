package trazzo.back.shared.security;

import java.util.List;
import java.util.UUID;

public interface TenantPermissionPort {
    List<String> findPermissionCodesByMasterUserId(UUID masterUserId);
}
