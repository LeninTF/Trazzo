package trazzo.back.incidents.infrastructure.adapters.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import trazzo.back.incidents.infrastructure.adapters.out.persistence.entity.IncidentPermissionEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentPermissionSpringDataRepository extends JpaRepository<IncidentPermissionEntity, String> {
    Optional<IncidentPermissionEntity> findByIncidentId(String incidentId);
    List<IncidentPermissionEntity> findByIncidentIdIn(List<String> incidentIds);
    void deleteByIncidentId(String incidentId);
}
