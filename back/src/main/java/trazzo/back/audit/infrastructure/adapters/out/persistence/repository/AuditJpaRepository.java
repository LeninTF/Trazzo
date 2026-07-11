package trazzo.back.audit.infrastructure.adapters.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import trazzo.back.audit.infrastructure.adapters.out.persistence.entity.AuditEntity;

import java.util.UUID;

@Repository
public interface AuditJpaRepository extends JpaRepository<AuditEntity, UUID> {
}
