package trazzo.back.organization.infrastructure.adapters.out.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import trazzo.back.organization.infrastructure.adapters.out.persistence.entity.AreaEntity;

@Repository
public interface AreaJpaRepository extends JpaRepository<AreaEntity, Long> {

    boolean existsByBranchIdAndName(Long branchId, String name);

    boolean existsByBranchIdAndNameAndIdNot(Long branchId, String name, Long id);

    @Query("SELECT a FROM AreaEntity a WHERE " +
           "(:branchId IS NULL OR a.branchId = :branchId) AND " +
           "(:state IS NULL OR a.state = :state) AND " +
           "(:search IS NULL OR LOWER(a.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<AreaEntity> findByFilters(
            @Param("branchId") Long branchId,
            @Param("state") Boolean state,
            @Param("search") String search,
            Pageable pageable
    );
}
