package trazzo.back.corehr.infrastructure.adapters.out.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.entity.DeviceEntity;

@Repository
public interface DeviceJpaRepository extends JpaRepository<DeviceEntity, Long> {

    boolean existsByCode(String code);

    @Query("SELECT d FROM DeviceEntity d WHERE " +
           "(:branchId IS NULL OR d.branchId = :branchId) AND " +
           "(:state IS NULL OR d.state = :state)")
    Page<DeviceEntity> findByBranchIdAndState(
            @Param("branchId") Long branchId,
            @Param("state") Boolean state,
            Pageable pageable);

    @Query("SELECT COUNT(d) FROM DeviceEntity d WHERE " +
           "(:branchId IS NULL OR d.branchId = :branchId) AND " +
           "(:state IS NULL OR d.state = :state)")
    long countByBranchIdAndState(
            @Param("branchId") Long branchId,
            @Param("state") Boolean state);
}
