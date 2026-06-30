package trazzo.back.organization.infrastructure.adapters.out.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import trazzo.back.organization.application.port.out.RolePermissionsRepositoryPort;
import trazzo.back.organization.domain.model.roles.RolePermissions;
import trazzo.back.organization.infrastructure.adapters.out.persistence.mapper.OrgMapper;
import trazzo.back.organization.infrastructure.adapters.out.persistence.repository.RolePermissionsJpaRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RolePermissionsRepositoryAdapter implements RolePermissionsRepositoryPort {

    private final RolePermissionsJpaRepository rolePermissionsRepo;

    @Override
    public RolePermissions save(RolePermissions rolePermissions) {
        return OrgMapper.toDomain(rolePermissionsRepo.save(OrgMapper.toEntity(rolePermissions)));
    }

    @Override
    public List<RolePermissions> findByRoleId(String roleId) {
        return rolePermissionsRepo.findByIdRoleId(roleId)
                .stream().map(OrgMapper::toDomain).toList();
    }

    @Override
    public boolean existsByRoleIdAndPermissionId(String roleId, String permissionId) {
        return rolePermissionsRepo.existsByIdRoleIdAndIdPermissionId(roleId, permissionId);
    }

    @Override
    public void deleteByRoleIdAndPermissionId(String roleId, String permissionId) {
        rolePermissionsRepo.deleteByIdRoleIdAndIdPermissionId(roleId, permissionId);
    }
}
