package trazzo.back.organization.application.usecase;

import lombok.RequiredArgsConstructor;
import trazzo.back.organization.application.dto.command.CreateRoleCommand;
import trazzo.back.organization.application.dto.command.UpdateRoleCommand;
import trazzo.back.organization.application.dto.result.PaginatedResult;
import trazzo.back.organization.application.dto.result.RoleResult;
import trazzo.back.organization.application.port.in.RoleUseCase;
import trazzo.back.organization.application.port.out.RoleRepositoryPort;
import trazzo.back.organization.domain.exception.DuplicateOrgNameException;
import trazzo.back.organization.domain.exception.OrgNotFoundException;
import trazzo.back.organization.domain.model.roles.Role;

import java.util.Optional;

@RequiredArgsConstructor
public class RoleService implements RoleUseCase {

    private final RoleRepositoryPort roleRepository;

    @Override
    public RoleResult create(CreateRoleCommand command) {
        if (roleRepository.existsByName(command.name())) {
            throw new DuplicateOrgNameException("Role with name '" + command.name() + "' already exists");
        }
        var role = Role.create(command.name(), command.description());
        return toResult(roleRepository.save(role));
    }

    @Override
    public Optional<RoleResult> findById(String id) {
        return roleRepository.findById(id).map(this::toResult);
    }

    @Override
    public PaginatedResult<RoleResult> findAll(String search, int page, int size, String sort) {
        var items = roleRepository.findAll(search, page, size, sort);
        var total = roleRepository.count(search);
        return PaginatedResult.of(items.stream().map(this::toResult).toList(), page, size, total);
    }

    @Override
    public RoleResult update(String id, UpdateRoleCommand command) {
        var role = roleRepository.findById(id)
                .orElseThrow(() -> new OrgNotFoundException("Role not found: " + id));
        if (roleRepository.existsByNameAndIdNot(command.name(), id)) {
            throw new DuplicateOrgNameException("Role with name '" + command.name() + "' already exists");
        }
        role.update(command.name(), command.description());
        return toResult(roleRepository.save(role));
    }

    @Override
    public void delete(String id) {
        roleRepository.findById(id)
                .orElseThrow(() -> new OrgNotFoundException("Role not found: " + id));
        roleRepository.deleteById(id);
    }

    private RoleResult toResult(Role role) {
        return new RoleResult(role.getId(), role.getName(), role.getDescription(),
                role.getCreatedAt(), role.getUpdatedAt());
    }
}
