package trazzo.back.corehr.infrastructure.adapters.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.entity.BranchRefEntity;

@Repository
public interface BranchJpaRepository extends JpaRepository<BranchRefEntity, Long> {
}
