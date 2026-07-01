package trazzo.back.corehr.infrastructure.adapters.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.entity.TenantUserDepartmentEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantUserDepartmentJpaRepository extends JpaRepository<TenantUserDepartmentEntity, Long> {

    List<TenantUserDepartmentEntity> findByTenantUserId(Long tenantUserId);

    Optional<TenantUserDepartmentEntity> findByTenantUserIdAndDepartmentId(Long tenantUserId, Long departmentId);

    Optional<TenantUserDepartmentEntity> findByTenantUserIdAndIsPrimaryTrue(Long tenantUserId);
}
