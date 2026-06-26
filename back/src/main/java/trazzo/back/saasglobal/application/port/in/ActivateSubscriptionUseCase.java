package trazzo.back.saasglobal.application.port.in;

import trazzo.back.saasglobal.application.dto.command.ActivateSubscriptionCommand;
import trazzo.back.saasglobal.application.dto.result.TenantResultDto;

public interface ActivateSubscriptionUseCase {
    TenantResultDto activate(ActivateSubscriptionCommand command);
}
