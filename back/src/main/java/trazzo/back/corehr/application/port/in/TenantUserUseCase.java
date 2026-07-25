package trazzo.back.corehr.application.port.in;

import trazzo.back.corehr.application.dto.command.CreateTenantUserCommand;
import trazzo.back.corehr.application.dto.command.PatchTenantUserCommand;
import trazzo.back.corehr.application.dto.result.PaginatedResult;
import trazzo.back.corehr.application.dto.result.SoftDeleteResult;
import trazzo.back.corehr.application.dto.result.TenantUserProfileResult;

import java.util.Optional;

public interface TenantUserUseCase {
    PaginatedResult<TenantUserProfileResult> findAll(String search, String status, int page, int size, String sort);
    Optional<TenantUserProfileResult> findById(Long id);

    TenantUserProfileResult create(CreateTenantUserCommand command);
    TenantUserProfileResult update(Long id, CreateTenantUserCommand command);
    TenantUserProfileResult patch(Long id, PatchTenantUserCommand command);
    SoftDeleteResult delete(Long id);

    TenantUserProfileResult assignRole(Long id, String roleId);
    void changePassword(Long id, String currentPassword, String newPassword);

    Optional<TenantUserProfileResult> findMe(String masterUserId);
    TenantUserProfileResult patchMe(String masterUserId, String phone, String imgUrl);
}
