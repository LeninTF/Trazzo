package trazzo.back.saasglobal.application.port.in;

import java.util.List;
import trazzo.back.saasglobal.application.dto.command.CreateRoleCommand;
import trazzo.back.saasglobal.application.dto.command.UpdateRoleCommand;
import trazzo.back.saasglobal.application.dto.command.UpdateRolePermissionsCommand;
import trazzo.back.saasglobal.application.dto.result.SaasRoleResult;

public interface SaasRoleUseCase {
    List<SaasRoleResult> listAll();
    SaasRoleResult getById(Integer id);
    SaasRoleResult create(CreateRoleCommand command);
    SaasRoleResult update(UpdateRoleCommand command);
    void deleteById(Integer id);
    SaasRoleResult updatePermissions(UpdateRolePermissionsCommand command);
}
