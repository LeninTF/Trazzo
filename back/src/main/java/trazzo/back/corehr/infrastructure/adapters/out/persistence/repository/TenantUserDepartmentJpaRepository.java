package trazzo.back.corehr.infrastructure.adapters.out.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.entity.TenantUserDepartmentEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantUserDepartmentJpaRepository extends JpaRepository<TenantUserDepartmentEntity, Long> {

    List<TenantUserDepartmentEntity> findByTenantUserId(Long tenantUserId);

    Page<TenantUserDepartmentEntity> findByTenantUserId(Long tenantUserId, Pageable pageable);

    long countByTenantUserId(Long tenantUserId);

    Optional<TenantUserDepartmentEntity> findByTenantUserIdAndDepartmentId(Long tenantUserId, Long departmentId);

    Optional<TenantUserDepartmentEntity> findByTenantUserIdAndIsPrimaryTrue(Long tenantUserId);
}
