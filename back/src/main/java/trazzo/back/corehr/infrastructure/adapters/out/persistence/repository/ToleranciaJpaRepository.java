package trazzo.back.corehr.infrastructure.adapters.out.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import trazzo.back.corehr.domain.model.ToleranciaType;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.entity.ToleranciaEntity;

import java.util.Optional;

@Repository
public interface ToleranciaJpaRepository extends JpaRepository<ToleranciaEntity, Long> {

    Page<ToleranciaEntity> findByScheduleId(Long scheduleId, Pageable pageable);

    long countByScheduleId(Long scheduleId);

    boolean existsByScheduleIdAndTypeAndActivoTrue(Long scheduleId, ToleranciaType type);

    Optional<ToleranciaEntity> findByScheduleIdAndId(Long scheduleId, Long id);
}
