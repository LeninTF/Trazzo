package trazzo.back.saasglobal.application.port.in;

import trazzo.back.saasglobal.application.dto.command.SubscribeToPlanCommand;
import trazzo.back.saasglobal.application.dto.result.SubscribeToPlanResult;

public interface SubscribeToPlanUseCase {
    SubscribeToPlanResult subscribe(SubscribeToPlanCommand command);
}
