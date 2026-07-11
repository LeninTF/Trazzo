package trazzo.back.audit.infrastructure.adapters.out.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import trazzo.back.audit.domain.model.master.Action;
import trazzo.back.audit.infrastructure.adapters.out.persistence.entity.AuditEntity;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface AuditJpaRepository extends JpaRepository<AuditEntity, UUID> {

    @Query("SELECT a FROM AuditEntity a WHERE " +
           "(:searchTerm IS NULL OR LOWER(a.entity) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(a.ipAdress) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "AND (:action IS NULL OR a.action = :action) " +
           "AND (:entity IS NULL OR a.entity = :entity) " +
           "AND (:fechaDesde IS NULL OR a.createdAt >= :fechaDesde) " +
           "AND (:fechaHasta IS NULL OR a.createdAt <= :fechaHasta)")
    Page<AuditEntity> findByFilters(
            @Param("searchTerm") String searchTerm,
            @Param("action") Action action,
            @Param("entity") String entity,
            @Param("fechaDesde") LocalDateTime fechaDesde,
            @Param("fechaHasta") LocalDateTime fechaHasta,
            Pageable pageable);
}
