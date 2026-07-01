package trazzo.back.corehr.application.port.in;

import trazzo.back.corehr.application.dto.command.CreateTenantUserDepartmentCommand;
import trazzo.back.corehr.application.dto.command.PatchTenantUserDepartmentCommand;
import trazzo.back.corehr.application.dto.result.TenantUserDepartmentResult;

import java.util.List;

public interface TenantUserDepartmentUseCase {
    TenantUserDepartmentResult create(Long tenantUserId, CreateTenantUserDepartmentCommand command);
    List<TenantUserDepartmentResult> findAllByTenantUserId(Long tenantUserId);
    TenantUserDepartmentResult patch(Long tenantUserId, Long departamentoId, PatchTenantUserDepartmentCommand command);
}
