package trazzo.back.audit.infrastructure.adapters.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import trazzo.back.audit.infrastructure.adapters.out.persistence.entity.SystemAuditEntity;

@Repository
public interface SystemAuditJpaRepository extends JpaRepository<SystemAuditEntity, Long> {
}
