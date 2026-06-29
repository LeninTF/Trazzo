package trazzo.back.corehr.infrastructure.adapters.out.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.entity.ShiftEntity;

@Repository
public interface ShiftJpaRepository extends JpaRepository<ShiftEntity, Long> {

    boolean existsByName(String name);

    Page<ShiftEntity> findByNameContainingIgnoreCase(String search, Pageable pageable);

    long countByNameContainingIgnoreCase(String search);
}
