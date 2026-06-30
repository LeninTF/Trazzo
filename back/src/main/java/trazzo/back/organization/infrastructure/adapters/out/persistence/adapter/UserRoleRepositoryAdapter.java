package trazzo.back.organization.infrastructure.adapters.out.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import trazzo.back.organization.application.port.out.UserRoleRepositoryPort;
import trazzo.back.organization.domain.model.roles.TenantUserRole;
import trazzo.back.organization.infrastructure.adapters.out.persistence.mapper.OrgMapper;
import trazzo.back.organization.infrastructure.adapters.out.persistence.repository.TenantUserRoleJpaRepository;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserRoleRepositoryAdapter implements UserRoleRepositoryPort {

    private final TenantUserRoleJpaRepository tenantUserRoleRepo;

    @Override
    public TenantUserRole save(TenantUserRole tenantUserRole) {
        return OrgMapper.toDomain(tenantUserRoleRepo.save(OrgMapper.toEntity(tenantUserRole)));
    }

    @Override
    public Optional<TenantUserRole> findById(Long id) {
        return tenantUserRoleRepo.findById(id).map(OrgMapper::toDomain);
    }

    @Override
    public List<TenantUserRole> findByTenantUserId(Long tenantUserId) {
        return tenantUserRoleRepo.findByTenantUserId(tenantUserId)
                .stream().map(OrgMapper::toDomain).toList();
    }

    @Override
    public List<TenantUserRole> findByRoleId(String roleId) {
        return tenantUserRoleRepo.findByRoleId(roleId)
                .stream().map(OrgMapper::toDomain).toList();
    }

    @Override
    public boolean existsByTenantUserIdAndRoleIdAndDepartmentId(Long tenantUserId, String roleId, Long departmentId) {
        return tenantUserRoleRepo.existsByTenantUserIdAndRoleIdAndDepartmentId(tenantUserId, roleId, departmentId);
    }

    @Override
    public void deleteById(Long id) {
        tenantUserRoleRepo.deleteById(id);
    }
}
