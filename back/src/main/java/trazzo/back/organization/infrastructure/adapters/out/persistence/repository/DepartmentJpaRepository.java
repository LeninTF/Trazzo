package trazzo.back.organization.infrastructure.adapters.out.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import trazzo.back.organization.infrastructure.adapters.out.persistence.entity.DepartmentEntity;

@Repository
public interface DepartmentJpaRepository extends JpaRepository<DepartmentEntity, Long> {

    boolean existsByAreaIdAndName(Long areaId, String name);

    boolean existsByAreaIdAndNameAndIdNot(Long areaId, String name, Long id);

    @Query("SELECT d FROM DepartmentEntity d WHERE " +
           "(:areaId IS NULL OR d.areaId = :areaId) AND " +
           "(:state IS NULL OR d.state = :state) AND " +
           "(:searchPattern IS NULL OR LOWER(d.name) LIKE :searchPattern)")
    Page<DepartmentEntity> findByFilters(
            @Param("areaId") Long areaId,
            @Param("state") Boolean state,
            @Param("searchPattern") String searchPattern,
            Pageable pageable
    );
}
