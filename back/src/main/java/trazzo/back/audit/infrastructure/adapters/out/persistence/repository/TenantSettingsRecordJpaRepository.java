package trazzo.back.audit.infrastructure.adapters.out.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import trazzo.back.audit.infrastructure.adapters.out.persistence.entity.TenantSettingsRecordEntity;

import java.time.LocalDateTime;

@Repository
public interface TenantSettingsRecordJpaRepository extends JpaRepository<TenantSettingsRecordEntity, Long> {

    @Query("SELECT t FROM TenantSettingsRecordEntity t WHERE " +
           "(:tenantSettingId IS NULL OR t.tenantSettingId = :tenantSettingId) AND " +
           "(:userId IS NULL OR t.userId = :userId) AND " +
           "(:changeReason IS NULL OR LOWER(t.changeReason) LIKE LOWER(CONCAT('%', :changeReason, '%'))) AND " +
           "(:fechaDesde IS NULL OR t.createdAt >= :fechaDesde) AND " +
           "(:fechaHasta IS NULL OR t.createdAt <= :fechaHasta)")
    Page<TenantSettingsRecordEntity> findByFilters(
            @Param("tenantSettingId") String tenantSettingId,
            @Param("userId") String userId,
            @Param("changeReason") String changeReason,
            @Param("fechaDesde") LocalDateTime fechaDesde,
            @Param("fechaHasta") LocalDateTime fechaHasta,
            Pageable pageable);
}
