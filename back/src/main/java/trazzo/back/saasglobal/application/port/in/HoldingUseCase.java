package trazzo.back.saasglobal.application.port.in;

import java.util.List;
import trazzo.back.saasglobal.application.dto.command.CreateHoldingCommand;
import trazzo.back.saasglobal.application.dto.command.UpdateHoldingCommand;
import trazzo.back.saasglobal.application.dto.result.HoldingResult;

public interface HoldingUseCase {
    HoldingResult create(CreateHoldingCommand command);
    HoldingResult getById(Integer id);
    List<HoldingResult> listAll();
    HoldingResult update(UpdateHoldingCommand command);
    HoldingResult activate(Integer id);
    HoldingResult deactivate(Integer id);
    void deleteById(Integer id);
}
