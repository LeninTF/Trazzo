package trazzo.back.organization.infrastructure.adapters.out.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import trazzo.back.organization.infrastructure.adapters.out.persistence.entity.BranchEntity;

@Repository
public interface BranchJpaRepository extends JpaRepository<BranchEntity, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    @Query("SELECT b FROM BranchEntity b WHERE " +
           "(:state IS NULL OR b.state = :state) AND " +
           "(:searchPattern IS NULL OR LOWER(b.name) LIKE :searchPattern)")
    Page<BranchEntity> findByFilters(
            @Param("state") Boolean state,
            @Param("searchPattern") String searchPattern,
            Pageable pageable
    );
}
