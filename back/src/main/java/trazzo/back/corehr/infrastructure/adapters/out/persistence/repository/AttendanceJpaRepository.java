package trazzo.back.corehr.infrastructure.adapters.out.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import trazzo.back.corehr.domain.model.AttendanceState;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.entity.AttendanceEntity;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface AttendanceJpaRepository extends JpaRepository<AttendanceEntity, String> {

    Optional<AttendanceEntity> findByTenantUserIdAndAttendanceDate(Long tenantUserId, LocalDate attendanceDate);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM AttendanceEntity a " +
           "WHERE a.offlineEventId = :offlineEventId AND a.deviceCode = :deviceCode")
    boolean existsByOfflineEventIdAndDeviceCode(
            @Param("offlineEventId") Integer offlineEventId,
            @Param("deviceCode") String deviceCode);

    @Query("SELECT a FROM AttendanceEntity a WHERE " +
           "(:tenantUserId IS NULL OR a.tenantUserId = :tenantUserId) AND " +
           "(:state IS NULL OR a.state = :state) AND " +
           "(:dateFrom IS NULL OR a.attendanceDate >= :dateFrom) AND " +
           "(:dateTo IS NULL OR a.attendanceDate <= :dateTo)")
    Page<AttendanceEntity> findByFilters(
            @Param("tenantUserId") Long tenantUserId,
            @Param("state") AttendanceState state,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            Pageable pageable);

    @Query("SELECT COUNT(a) FROM AttendanceEntity a WHERE " +
           "(:tenantUserId IS NULL OR a.tenantUserId = :tenantUserId) AND " +
           "(:state IS NULL OR a.state = :state) AND " +
           "(:dateFrom IS NULL OR a.attendanceDate >= :dateFrom) AND " +
           "(:dateTo IS NULL OR a.attendanceDate <= :dateTo)")
    long countByFilters(
            @Param("tenantUserId") Long tenantUserId,
            @Param("state") AttendanceState state,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo);
}
