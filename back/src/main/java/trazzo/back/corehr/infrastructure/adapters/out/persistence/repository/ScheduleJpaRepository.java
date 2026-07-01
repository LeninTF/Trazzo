package trazzo.back.corehr.infrastructure.adapters.out.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.entity.ScheduleEntity;

@Repository
public interface ScheduleJpaRepository extends JpaRepository<ScheduleEntity, Long> {

    Page<ScheduleEntity> findByShiftId(Long shiftId, Pageable pageable);

    long countByShiftId(Long shiftId);

    boolean existsByShiftId(Long shiftId);
}
