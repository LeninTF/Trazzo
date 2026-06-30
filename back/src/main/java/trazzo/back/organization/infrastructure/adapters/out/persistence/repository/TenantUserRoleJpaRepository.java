package trazzo.back.organization.infrastructure.adapters.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import trazzo.back.organization.infrastructure.adapters.out.persistence.entity.TenantUserRoleEntity;

import java.util.List;

@Repository
public interface TenantUserRoleJpaRepository extends JpaRepository<TenantUserRoleEntity, Long> {

    List<TenantUserRoleEntity> findByTenantUserId(Long tenantUserId);

    List<TenantUserRoleEntity> findByRoleId(String roleId);

    boolean existsByTenantUserIdAndRoleIdAndDepartmentId(Long tenantUserId, String roleId, Long departmentId);
}
