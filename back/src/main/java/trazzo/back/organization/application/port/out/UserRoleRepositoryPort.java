package trazzo.back.organization.application.port.out;

import trazzo.back.organization.domain.model.roles.TenantUserRole;

import java.util.List;
import java.util.Optional;

public interface UserRoleRepositoryPort {
    TenantUserRole save(TenantUserRole tenantUserRole);
    Optional<TenantUserRole> findById(Long id);
    List<TenantUserRole> findByTenantUserId(Long tenantUserId);
    List<TenantUserRole> findByRoleId(String roleId);
    boolean existsByTenantUserIdAndRoleIdAndDepartmentId(Long tenantUserId, String roleId, Long departmentId);
    void deleteById(Long id);
}
