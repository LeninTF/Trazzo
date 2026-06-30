package trazzo.back.audit.infrastructure.adapters.out.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import trazzo.back.audit.infrastructure.adapters.out.persistence.entity.SessionEntity;

@Repository
public interface SessionJpaRepository extends JpaRepository<SessionEntity, Long> {

    @Query("SELECT s FROM SessionEntity s WHERE " +
           "(:tenantUserId IS NULL OR s.tenantUserId = :tenantUserId) AND " +
           "(:state IS NULL OR s.state = :state) AND " +
           "(:ipAddress IS NULL OR s.ipAddress = :ipAddress)")
    Page<SessionEntity> findByFilters(@Param("tenantUserId") String tenantUserId, @Param("state") Boolean state, @Param("ipAddress") String ipAddress, Pageable pageable);

    @Query("SELECT COUNT(s) FROM SessionEntity s WHERE " +
           "(:tenantUserId IS NULL OR s.tenantUserId = :tenantUserId) AND " +
           "(:state IS NULL OR s.state = :state) AND " +
           "(:ipAddress IS NULL OR s.ipAddress = :ipAddress)")
    long countByFilters(@Param("tenantUserId") String tenantUserId, @Param("state") Boolean state, @Param("ipAddress") String ipAddress);
}
