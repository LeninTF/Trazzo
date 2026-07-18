package trazzo.back.saasglobal.application.port.in;

import trazzo.back.saasglobal.application.dto.command.AssignSaasUserRolesCommand;
import trazzo.back.saasglobal.application.dto.command.CreateSaasUserCommand;
import trazzo.back.saasglobal.application.dto.command.UpdateSaasUserCommand;
import trazzo.back.saasglobal.application.dto.result.PaginatedResult;
import trazzo.back.saasglobal.application.dto.result.SaasUserResult;

public interface SaasUserUseCase {
    PaginatedResult<SaasUserResult> listAll(String search, int page, int size);
    SaasUserResult getById(String id);
    SaasUserResult create(CreateSaasUserCommand command);
    SaasUserResult update(UpdateSaasUserCommand command);
    void deleteById(String id);
    SaasUserResult assignRoles(AssignSaasUserRolesCommand command);
}
