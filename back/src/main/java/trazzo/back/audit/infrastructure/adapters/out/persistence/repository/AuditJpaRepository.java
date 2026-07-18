package trazzo.back.audit.infrastructure.adapters.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import trazzo.back.audit.infrastructure.adapters.out.persistence.entity.AuditEntity;

import java.util.UUID;

@Repository
public interface AuditJpaRepository extends JpaRepository<AuditEntity, UUID> {

    // Every optional-filter parameter is CAST explicitly, including its own bare "IS NULL"
    // check. Hibernate renders each JPQL reference to a named parameter as a separate `?` in
    // the final SQL, so PostgreSQL infers each occurrence's type independently; a bare
    // "? IS NULL" with no other type context in that specific occurrence can fail to prepare
    // ("could not determine data type of parameter") even though the same-named parameter is
    // typed correctly elsewhere in the query.
    @Query("SELECT a FROM AuditEntity a WHERE " +
           "(CAST(:searchTerm AS string) IS NULL OR LOWER(a.entity) LIKE LOWER(CONCAT('%', CAST(:searchTerm AS string), '%')) " +
           "OR LOWER(a.ipAdress) LIKE LOWER(CONCAT('%', CAST(:searchTerm AS string), '%'))) " +
           "AND (CAST(:action AS string) IS NULL OR a.action = :action) " +
           "AND (CAST(:entity AS string) IS NULL OR a.entity = :entity) " +
           "AND (CAST(CAST(:fechaDesde AS string) AS timestamp) IS NULL OR a.createdAt >= CAST(CAST(:fechaDesde AS string) AS timestamp)) " +
           "AND (CAST(CAST(:fechaHasta AS string) AS timestamp) IS NULL OR a.createdAt <= CAST(CAST(:fechaHasta AS string) AS timestamp))")
    Page<AuditEntity> findByFilters(
            @Param("searchTerm") String searchTerm,
            @Param("action") Action action,
            @Param("entity") String entity,
            @Param("fechaDesde") LocalDateTime fechaDesde,
            @Param("fechaHasta") LocalDateTime fechaHasta,
            Pageable pageable);
}
