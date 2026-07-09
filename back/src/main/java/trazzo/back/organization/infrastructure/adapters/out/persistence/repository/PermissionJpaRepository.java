package trazzo.back.organization.infrastructure.adapters.out.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import trazzo.back.organization.infrastructure.adapters.out.persistence.entity.PermissionEntity;

import java.util.UUID;

@Repository
public interface PermissionJpaRepository extends JpaRepository<PermissionEntity, UUID> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, UUID id);

    @Query("SELECT p FROM PermissionEntity p WHERE " +
           "(:searchPattern IS NULL OR LOWER(p.name) LIKE :searchPattern)")
    Page<PermissionEntity> findByFilters(
            @Param("searchPattern") String searchPattern,
            Pageable pageable
    );
}
