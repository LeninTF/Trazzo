package trazzo.back.incidents.infrastructure.adapters.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import trazzo.back.incidents.infrastructure.adapters.out.persistence.entity.IncidentEntity;

@Repository
public interface IncidentSpringDataRepository
        extends JpaRepository<IncidentEntity, Integer>, JpaSpecificationExecutor<IncidentEntity> {
}
