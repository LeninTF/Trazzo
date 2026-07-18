package trazzo.back.saasglobal.application.usecase;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import trazzo.back.saasglobal.application.dto.command.CreateRoleCommand;
import trazzo.back.saasglobal.application.dto.command.UpdateRoleCommand;
import trazzo.back.saasglobal.application.dto.command.UpdateRolePermissionsCommand;
import trazzo.back.saasglobal.application.dto.result.SaasRoleResult;
import trazzo.back.saasglobal.application.port.in.SaasRoleUseCase;
import trazzo.back.saasglobal.application.port.out.RoleMasterRepositoryPort;
import trazzo.back.saasglobal.domain.exception.RoleInUseException;
import trazzo.back.saasglobal.domain.model.iam.RoleMaster;

@Service
@RequiredArgsConstructor
public class SaasRoleService implements SaasRoleUseCase {

    private static final String NOT_FOUND_MSG = "Role not found: ";
    private static final String SYSTEM_MANAGED_ROLE = "admin_trazzo";

    private final RoleMasterRepositoryPort roleRepository;

    @Override
    public List<SaasRoleResult> listAll() {
        return roleRepository.findAll().stream().map(this::toResult).toList();
    }

    @Override
    public SaasRoleResult getById(Integer id) {
        return roleRepository.findById(id)
                .map(this::toResult)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND_MSG + id));
    }

    @Override
    public SaasRoleResult create(CreateRoleCommand command) {
        RoleMaster role = RoleMaster.create(command.name(), command.displayName(), command.description());
        return toResult(roleRepository.save(role));
    }

    @Override
    public SaasRoleResult update(UpdateRoleCommand command) {
        RoleMaster role = roleRepository.findById(command.id())
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND_MSG + command.id()));
        if (SYSTEM_MANAGED_ROLE.equals(role.getName())) {
            throw new RoleInUseException("El rol " + SYSTEM_MANAGED_ROLE + " no se puede editar");
        }
        role.update(command.name(), command.displayName(), command.description());
        return toResult(roleRepository.save(role));
    }

    @Override
    public void deleteById(Integer id) {
        RoleMaster role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND_MSG + id));
        if (SYSTEM_MANAGED_ROLE.equals(role.getName())) {
            throw new RoleInUseException("El rol " + SYSTEM_MANAGED_ROLE + " no se puede eliminar");
        }
        if (roleRepository.isAssignedToAnyUser(id)) {
            throw new RoleInUseException("El rol tiene usuarios asignados y no se puede eliminar");
        }
        roleRepository.deleteById(id);
    }

    @Override
    public SaasRoleResult updatePermissions(UpdateRolePermissionsCommand command) {
        RoleMaster role = roleRepository.findById(command.roleId())
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND_MSG + command.roleId()));
        role.grantPermissions(command.permissions());
        roleRepository.replacePermissions(role.getId(), role.getPermissionCodes());
        return getById(role.getId());
    }

    private SaasRoleResult toResult(RoleMaster role) {
        return new SaasRoleResult(role.getId(), role.getName(), role.getDisplayName(), role.getDescription(),
                role.getPermissionCodes(), SYSTEM_MANAGED_ROLE.equals(role.getName()));
    }
}
