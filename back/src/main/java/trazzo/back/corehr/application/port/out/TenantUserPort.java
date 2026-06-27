package trazzo.back.corehr.application.port.out;

import trazzo.back.corehr.domain.model.TenantUserState;

import java.util.Optional;

public interface TenantUserPort {
    Optional<TenantUserBasicInfo> findBasicInfoById(Long tenantUserId);
    Optional<TenantUserState> findStateById(Long tenantUserId);
    boolean existsById(Long tenantUserId);

    record TenantUserBasicInfo(Long id, String nombre, String apellidoPaterno,
                               String apellidoMaterno, String email) {
    }
}
