package trazzo.back.organization.infrastructure.adapters.out.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import trazzo.back.organization.infrastructure.adapters.out.persistence.entity.RoleEntity;

@Repository
public interface RoleJpaRepository extends JpaRepository<RoleEntity, String> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, String id);

    @Query("SELECT r FROM RoleEntity r WHERE " +
           "(:search IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<RoleEntity> findByFilters(
            @Param("search") String search,
            Pageable pageable
    );
}
