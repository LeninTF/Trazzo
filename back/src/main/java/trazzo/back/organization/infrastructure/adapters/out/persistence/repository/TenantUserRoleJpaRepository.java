package trazzo.back.organization.infrastructure.adapters.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import trazzo.back.organization.infrastructure.adapters.out.persistence.entity.TenantUserRoleEntity;

import java.util.List;
import java.util.UUID;

@Repository
public interface TenantUserRoleJpaRepository extends JpaRepository<TenantUserRoleEntity, Long> {

    List<TenantUserRoleEntity> findByTenantUserId(Long tenantUserId);

    List<TenantUserRoleEntity> findByRoleId(UUID roleId);

    boolean existsByTenantUserIdAndRoleIdAndDepartmentId(Long tenantUserId, UUID roleId, Long departmentId);
}
