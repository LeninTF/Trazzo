package trazzo.back.saasglobal.application.port.in;

import trazzo.back.saasglobal.application.dto.command.CreateTrialTenantCommand;
import trazzo.back.saasglobal.application.dto.result.TenantResultDto;

public interface CreateTrialTenantUseCase {
    TenantResultDto createTrial(CreateTrialTenantCommand command);
}
