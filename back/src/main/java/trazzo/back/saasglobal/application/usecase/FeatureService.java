package trazzo.back.saasglobal.application.usecase;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import trazzo.back.saasglobal.application.dto.command.CreateFeatureCommand;
import trazzo.back.saasglobal.application.dto.result.FeatureResult;
import trazzo.back.saasglobal.application.port.in.FeatureUseCase;
import trazzo.back.saasglobal.application.port.out.FeatureRepositoryPort;
import trazzo.back.saasglobal.domain.model.multitenancy.Feature;

@Service
@RequiredArgsConstructor
public class FeatureService implements FeatureUseCase {

    private final FeatureRepositoryPort featureRepository;

    @Override
    public FeatureResult create(CreateFeatureCommand command) {
        Feature feature = Feature.create(command.name(), command.description());
        return toResult(featureRepository.save(feature));
    }

    @Override
    public FeatureResult getById(Integer id) {
        return featureRepository.findById(id)
                .map(this::toResult)
                .orElseThrow(() -> new IllegalArgumentException("Feature not found: " + id));
    }

    @Override
    public List<FeatureResult> listAll() {
        return featureRepository.findAll().stream().map(this::toResult).toList();
    }

    @Override
    public void deleteById(Integer id) {
        featureRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Feature not found: " + id));
        featureRepository.deleteById(id);
    }

    private FeatureResult toResult(Feature f) {
        return new FeatureResult(f.getId(), f.getName(), f.getDescription(),
                f.getCreatedAt(), f.getUpdatedAt());
    }
}
