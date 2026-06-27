package trazzo.back.saasglobal.application.port.in;

import java.util.List;
import trazzo.back.saasglobal.application.dto.result.PlanResult;

public interface PlanUseCase {
    PlanResult getById(Integer id);
    List<PlanResult> listActive();
    List<PlanResult> listAll();
}
