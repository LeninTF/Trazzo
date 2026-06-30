package trazzo.back.organization.application.usecase;

import lombok.RequiredArgsConstructor;
import trazzo.back.organization.application.dto.command.CreatePermissionCommand;
import trazzo.back.organization.application.dto.command.UpdatePermissionCommand;
import trazzo.back.organization.application.dto.result.PaginatedResult;
import trazzo.back.organization.application.dto.result.PermissionResult;
import trazzo.back.organization.application.port.in.PermissionUseCase;
import trazzo.back.organization.application.port.out.PermissionRepositoryPort;
import trazzo.back.organization.domain.exception.DuplicateOrgNameException;
import trazzo.back.organization.domain.exception.OrgNotFoundException;
import trazzo.back.organization.domain.model.roles.Permissions;

import java.util.Optional;

@RequiredArgsConstructor
public class PermissionService implements PermissionUseCase {

    private final PermissionRepositoryPort permissionRepository;

    @Override
    public PermissionResult create(CreatePermissionCommand command) {
        if (permissionRepository.existsByName(command.name())) {
            throw new DuplicateOrgNameException("Permission with name '" + command.name() + "' already exists");
        }
        var permission = Permissions.create(command.name(), command.description(), command.masterFeaturesCode());
        return toResult(permissionRepository.save(permission));
    }

    @Override
    public Optional<PermissionResult> findById(String id) {
        return permissionRepository.findById(id).map(this::toResult);
    }

    @Override
    public PaginatedResult<PermissionResult> findAll(String search, int page, int size, String sort) {
        var items = permissionRepository.findAll(search, page, size, sort);
        var total = permissionRepository.count(search);
        var totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        return new PaginatedResult<>(items.stream().map(this::toResult).toList(), page, size, total, totalPages);
    }

    @Override
    public PermissionResult update(String id, UpdatePermissionCommand command) {
        var permission = permissionRepository.findById(id)
                .orElseThrow(() -> new OrgNotFoundException("Permission not found: " + id));
        if (permissionRepository.existsByNameAndIdNot(command.name(), id)) {
            throw new DuplicateOrgNameException("Permission with name '" + command.name() + "' already exists");
        }
        permission.update(command.name(), command.description(), command.masterFeaturesCode());
        return toResult(permissionRepository.save(permission));
    }

    @Override
    public void delete(String id) {
        permissionRepository.findById(id)
                .orElseThrow(() -> new OrgNotFoundException("Permission not found: " + id));
        permissionRepository.deleteById(id);
    }

    private PermissionResult toResult(Permissions p) {
        return new PermissionResult(p.getId(), p.getName(), p.getDescription(),
                p.getMasterFeaturesCode(), p.getCreatedAt(), p.getUpdatedAt());
    }
}
