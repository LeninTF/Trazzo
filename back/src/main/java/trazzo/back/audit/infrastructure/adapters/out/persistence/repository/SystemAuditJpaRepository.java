package trazzo.back.audit.infrastructure.adapters.out.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import trazzo.back.audit.infrastructure.adapters.out.persistence.entity.SystemAuditEntity;

import java.time.LocalDateTime;

@Repository
public interface SystemAuditJpaRepository extends JpaRepository<SystemAuditEntity, Long> {

    @Query("SELECT s FROM SystemAuditEntity s WHERE " +
           "(:searchTerm IS NULL OR :searchTerm = '' OR LOWER(s.modulo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(s.descripcion) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(s.endpoint) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(s.ipAddress) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
           "(:module IS NULL OR s.modulo = :module) AND " +
           "(:fechaDesde IS NULL OR s.date >= :fechaDesde) AND " +
           "(:fechaHasta IS NULL OR s.date <= :fechaHasta)")
    Page<SystemAuditEntity> findByFilters(@Param("searchTerm") String searchTerm, @Param("module") String module, @Param("fechaDesde") LocalDateTime fechaDesde, @Param("fechaHasta") LocalDateTime fechaHasta, Pageable pageable);

    @Query("SELECT COUNT(s) FROM SystemAuditEntity s WHERE " +
           "(:searchTerm IS NULL OR :searchTerm = '' OR LOWER(s.modulo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(s.descripcion) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(s.endpoint) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(s.ipAddress) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
           "(:module IS NULL OR s.modulo = :module) AND " +
           "(:fechaDesde IS NULL OR s.date >= :fechaDesde) AND " +
           "(:fechaHasta IS NULL OR s.date <= :fechaHasta)")
    long countByFilters(@Param("searchTerm") String searchTerm, @Param("module") String module, @Param("fechaDesde") LocalDateTime fechaDesde, @Param("fechaHasta") LocalDateTime fechaHasta);
}
