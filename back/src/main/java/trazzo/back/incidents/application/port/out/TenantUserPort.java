package trazzo.back.incidents.application.port.out;

import java.util.Optional;

public interface TenantUserPort {
    Optional<TenantUserBasicInfo> findBasicInfoById(String tenantUserId);

    record TenantUserBasicInfo(String id, String nombre, String apellidoPaterno,
                                String apellidoMaterno, String email) {
    }
}
