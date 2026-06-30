package trazzo.back.audit.infrastructure.adapters.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import trazzo.back.audit.infrastructure.adapters.out.persistence.entity.SessionEntity;

@Repository
public interface SessionJpaRepository extends JpaRepository<SessionEntity, Long> {
}
