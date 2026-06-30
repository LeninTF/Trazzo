package trazzo.back.audit.infrastructure.adapters.out.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import trazzo.back.audit.domain.model.master.StatusLogin;
import trazzo.back.audit.infrastructure.adapters.out.persistence.entity.LogInHistoryEntity;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface LogInHistoryJpaRepository extends JpaRepository<LogInHistoryEntity, UUID> {

    @Query("SELECT l FROM LogInHistoryEntity l WHERE " +
           "(:userId IS NULL OR l.userId = :userId) AND " +
           "(:status IS NULL OR l.status = :status) AND " +
           "(:fechaDesde IS NULL OR l.createdAt >= :fechaDesde) AND " +
           "(:fechaHasta IS NULL OR l.createdAt <= :fechaHasta)")
    Page<LogInHistoryEntity> findByFilters(
            @Param("userId") UUID userId,
            @Param("status") StatusLogin status,
            @Param("fechaDesde") LocalDateTime fechaDesde,
            @Param("fechaHasta") LocalDateTime fechaHasta,
            Pageable pageable);
}
