package trazzo.back.organization.infrastructure.adapters.out.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import trazzo.back.organization.infrastructure.adapters.out.persistence.entity.PermissionEntity;

@Repository
public interface PermissionJpaRepository extends JpaRepository<PermissionEntity, String> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, String id);

    @Query("SELECT p FROM PermissionEntity p WHERE " +
           "(:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<PermissionEntity> findByFilters(
            @Param("search") String search,
            Pageable pageable
    );
}
