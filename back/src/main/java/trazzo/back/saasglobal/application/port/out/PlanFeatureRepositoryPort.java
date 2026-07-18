package trazzo.back.saasglobal.application.port.out;

import java.util.List;
import trazzo.back.saasglobal.domain.model.multitenancy.PlanFeature;

public interface PlanFeatureRepositoryPort {
    PlanFeature save(PlanFeature planFeature);
    List<PlanFeature> findByPlanId(Integer planId);
    void deleteByPlanId(Integer planId);
}
