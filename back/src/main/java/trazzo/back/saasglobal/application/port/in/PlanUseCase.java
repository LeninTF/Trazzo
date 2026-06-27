package trazzo.back.saasglobal.application.port.in;

import java.util.List;
import trazzo.back.saasglobal.application.dto.command.CreatePlanCommand;
import trazzo.back.saasglobal.application.dto.command.UpdatePlanCommand;
import trazzo.back.saasglobal.application.dto.result.PlanResult;

public interface PlanUseCase {
    PlanResult create(CreatePlanCommand command);
    PlanResult getById(Integer id);
    List<PlanResult> listActive();
    List<PlanResult> listAll();
    PlanResult update(UpdatePlanCommand command);
    PlanResult activate(Integer id);
    PlanResult deactivate(Integer id);
    void deleteById(Integer id);
}
