package trazzo.back.saasglobal.application.port.out;

import java.util.List;
import java.util.Optional;
import trazzo.back.saasglobal.domain.model.multitenancy.Feature;

public interface FeatureRepositoryPort {
    Feature save(Feature feature);
    Optional<Feature> findById(Integer id);
    List<Feature> findAll();
    void deleteById(Integer id);
}
