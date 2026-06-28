package trazzo.back.incidents.infrastructure.adapters.out.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import trazzo.back.incidents.infrastructure.adapters.out.persistence.entity.IncidentTypeEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentTypeSpringDataRepository extends JpaRepository<IncidentTypeEntity, String> {
    Page<IncidentTypeEntity> findByActivo(Boolean activo, Pageable pageable);
    Optional<IncidentTypeEntity> findByNombre(String nombre);
    boolean existsByNombre(String nombre);
    List<IncidentTypeEntity> findByIdIn(List<String> ids);
}
