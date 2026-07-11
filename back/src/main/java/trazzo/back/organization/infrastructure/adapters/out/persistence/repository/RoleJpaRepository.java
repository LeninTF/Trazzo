package trazzo.back.organization.infrastructure.adapters.out.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import trazzo.back.organization.infrastructure.adapters.out.persistence.entity.RoleEntity;

import java.util.UUID;

@Repository
public interface RoleJpaRepository extends JpaRepository<RoleEntity, UUID> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, UUID id);

    @Query("SELECT r FROM RoleEntity r WHERE " +
           "(:searchPattern IS NULL OR LOWER(r.name) LIKE :searchPattern)")
    Page<RoleEntity> findByFilters(
            @Param("searchPattern") String searchPattern,
            Pageable pageable
    );
}
