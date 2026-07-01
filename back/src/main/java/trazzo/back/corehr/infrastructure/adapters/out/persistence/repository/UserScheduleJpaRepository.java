package trazzo.back.corehr.infrastructure.adapters.out.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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

    List<UserScheduleEntity> findByTenantUserId(Long tenantUserId);
}
