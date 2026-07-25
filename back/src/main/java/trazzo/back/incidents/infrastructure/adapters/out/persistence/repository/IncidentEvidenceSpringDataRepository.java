package trazzo.back.incidents.infrastructure.adapters.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import trazzo.back.incidents.infrastructure.adapters.out.persistence.entity.IncidentEvidenceEntity;

import java.util.List;

@Repository
public interface IncidentEvidenceSpringDataRepository extends JpaRepository<IncidentEvidenceEntity, Integer> {
    List<IncidentEvidenceEntity> findByIncidentId(Integer incidentId);
    void deleteByIncidentId(Integer incidentId);
}
