package trazzo.back.incidents.infrastructure.adapters.out.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import trazzo.back.incidents.infrastructure.adapters.out.persistence.entity.IncidentEntity;

import java.time.LocalDateTime;

@Repository
public interface IncidentSpringDataRepository extends JpaRepository<IncidentEntity, String> {

    @Query("SELECT i FROM IncidentEntity i WHERE " +
           "(:tenantUserId IS NULL OR i.tenantUserId = :tenantUserId) AND " +
           "(:state IS NULL OR i.state = :state) AND " +
           "(:tipoId IS NULL OR i.incidentTypeId = :tipoId) AND " +
           "(:desde IS NULL OR i.createdAt >= :desde) AND " +
           "(:hasta IS NULL OR i.createdAt <= :hasta) AND " +
           "(:search IS NULL OR LOWER(i.comment) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<IncidentEntity> findByFilters(
            @Param("tenantUserId") String tenantUserId,
            @Param("state") String state,
            @Param("tipoId") String tipoId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            @Param("search") String search,
            Pageable pageable
    );
}
