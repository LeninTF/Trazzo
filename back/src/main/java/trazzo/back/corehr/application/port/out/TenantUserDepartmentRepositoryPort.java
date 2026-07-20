package trazzo.back.corehr.application.port.out;

import trazzo.back.corehr.domain.model.employee.TenantUserDepartment;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;

public interface TenantUserDepartmentRepositoryPort {
    TenantUserDepartment save(TenantUserDepartment department);
    Optional<TenantUserDepartment> findById(Long id);
    List<TenantUserDepartment> findAllByTenantUserId(Long tenantUserId);
    Page<TenantUserDepartment> findAllByTenantUserId(Long tenantUserId, int page, int size);
    long countByTenantUserId(Long tenantUserId);
    Optional<TenantUserDepartment> findByTenantUserIdAndDepartmentId(Long tenantUserId, Long departmentId);
    Optional<TenantUserDepartment> findPrimaryByTenantUserId(Long tenantUserId);
    void deleteById(Long id);
}
