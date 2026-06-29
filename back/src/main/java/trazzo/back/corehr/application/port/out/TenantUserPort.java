package trazzo.back.corehr.application.port.out;

import trazzo.back.corehr.domain.model.TenantUserState;

import java.util.Optional;

public interface TenantUserPort {
    Optional<TenantUserBasicInfo> findBasicInfoById(String tenantUserId);
    Optional<TenantUserState> findStateById(String tenantUserId);
    boolean existsById(String tenantUserId);

    record TenantUserBasicInfo(String id, String nombre, String apellidoPaterno,
                                String apellidoMaterno, String email) {
    }
}
