package trazzo.back.corehr.infrastructure.adapters.out.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.entity.UserScheduleEntity;

import java.util.List;

@Repository
public interface UserScheduleJpaRepository extends JpaRepository<UserScheduleEntity, Long> {

    @Query("SELECT u FROM UserScheduleEntity u WHERE " +
           "(:tenantUserId IS NULL OR u.tenantUserId = :tenantUserId) AND " +
           "(:scheduleId IS NULL OR u.scheduleId = :scheduleId)")
    Page<UserScheduleEntity> findByTenantUserIdAndScheduleId(
            @Param("tenantUserId") Long tenantUserId,
            @Param("scheduleId") Long scheduleId,
            Pageable pageable);

    @Query("SELECT COUNT(u) FROM UserScheduleEntity u WHERE " +
           "(:tenantUserId IS NULL OR u.tenantUserId = :tenantUserId) AND " +
           "(:scheduleId IS NULL OR u.scheduleId = :scheduleId)")
    long countByTenantUserIdAndScheduleId(
            @Param("tenantUserId") Long tenantUserId,
            @Param("scheduleId") Long scheduleId);

    boolean existsByTenantUserId(Long tenantUserId);

    boolean existsByTenantUserIdAndScheduleId(Long tenantUserId, Long scheduleId);

    List<UserScheduleEntity> findByTenantUserId(Long tenantUserId);

    @Query("""
            SELECT u FROM UserScheduleEntity u
            LEFT JOIN ScheduleEntity s ON s.id = u.scheduleId
            WHERE (:tenantUserId IS NULL OR u.tenantUserId = :tenantUserId)
              AND (:scheduleId IS NULL OR u.scheduleId = :scheduleId)
              AND (:shiftId IS NULL OR s.shiftId = :shiftId)
            """)
    Page<UserScheduleEntity> findAllFiltered(
            @Param("tenantUserId") Long tenantUserId,
            @Param("scheduleId") Long scheduleId,
            @Param("shiftId") Long shiftId,
            Pageable pageable);

    @Query("""
            SELECT COUNT(u) FROM UserScheduleEntity u
            LEFT JOIN ScheduleEntity s ON s.id = u.scheduleId
            WHERE (:tenantUserId IS NULL OR u.tenantUserId = :tenantUserId)
              AND (:scheduleId IS NULL OR u.scheduleId = :scheduleId)
              AND (:shiftId IS NULL OR s.shiftId = :shiftId)
            """)
    long countFiltered(
            @Param("tenantUserId") Long tenantUserId,
            @Param("scheduleId") Long scheduleId,
            @Param("shiftId") Long shiftId);

    @Modifying
    @Query(value = """
            INSERT INTO user_schedule (tenant_user_id, schedule_id, description, entry_time, departure_time, created_at, updated_at)
            SELECT :tenantUserId, :scheduleId, :description, :entryTime, :departureTime, now(), now()
            WHERE NOT EXISTS (
                SELECT 1 FROM user_schedule us
                WHERE us.tenant_user_id = :tenantUserId AND us.schedule_id = :scheduleId
            )
            """, nativeQuery = true)
    void insertIfAbsent(
            @Param("tenantUserId") Long tenantUserId,
            @Param("scheduleId") Long scheduleId,
            @Param("description") String description,
            @Param("entryTime") java.time.LocalTime entryTime,
            @Param("departureTime") java.time.LocalTime departureTime);
}
