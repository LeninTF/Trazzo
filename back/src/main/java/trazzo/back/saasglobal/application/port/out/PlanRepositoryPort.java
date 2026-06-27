package trazzo.back.saasglobal.application.port.out;

import java.util.List;
import java.util.Optional;
import trazzo.back.saasglobal.domain.model.multitenancy.Plan;

public interface PlanRepositoryPort {
    Optional<Plan> findById(Integer id);
    List<Plan> findAll();
    List<Plan> findAllActive();
}
