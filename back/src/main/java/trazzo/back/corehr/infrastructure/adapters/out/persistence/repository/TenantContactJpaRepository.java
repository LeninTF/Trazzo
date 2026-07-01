package trazzo.back.corehr.infrastructure.adapters.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.entity.TenantContactEntity;

import java.util.List;

@Repository
public interface TenantContactJpaRepository extends JpaRepository<TenantContactEntity, Long> {

    List<TenantContactEntity> findByTenantUserId(Long tenantUserId);

    List<TenantContactEntity> findByTenantUserIdAndDeletedAtIsNull(Long tenantUserId);
}
