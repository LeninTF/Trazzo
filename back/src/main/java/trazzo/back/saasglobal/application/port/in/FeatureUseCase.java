package trazzo.back.saasglobal.application.port.in;

import java.util.List;
import trazzo.back.saasglobal.application.dto.command.CreateFeatureCommand;
import trazzo.back.saasglobal.application.dto.result.FeatureResult;

public interface FeatureUseCase {
    FeatureResult create(CreateFeatureCommand command);
    FeatureResult getById(Integer id);
    List<FeatureResult> listAll();
    void deleteById(Integer id);
}
