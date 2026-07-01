package trazzo.back.corehr.infrastructure.adapters.out.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import trazzo.back.corehr.application.port.out.TenantUserDepartmentRepositoryPort;
import trazzo.back.corehr.domain.model.employee.TenantUserDepartment;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.mapper.TenantUserDepartmentMapper;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.repository.TenantUserDepartmentJpaRepository;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TenantUserDepartmentRepositoryAdapter implements TenantUserDepartmentRepositoryPort {

    private final TenantUserDepartmentJpaRepository tenantUserDepartmentRepo;

    @Override
    @Transactional
    public TenantUserDepartment save(TenantUserDepartment department) {
        var entity = TenantUserDepartmentMapper.toEntity(department);
        var saved = tenantUserDepartmentRepo.save(entity);
        return TenantUserDepartmentMapper.toDomain(saved);
    }

    @Override
    public Optional<TenantUserDepartment> findById(Long id) {
        return tenantUserDepartmentRepo.findById(id).map(TenantUserDepartmentMapper::toDomain);
    }

    @Override
    public List<TenantUserDepartment> findAllByTenantUserId(Long tenantUserId) {
        return tenantUserDepartmentRepo.findByTenantUserId(tenantUserId)
                .stream()
                .map(TenantUserDepartmentMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<TenantUserDepartment> findByTenantUserIdAndDepartmentId(Long tenantUserId, Long departmentId) {
        return tenantUserDepartmentRepo.findByTenantUserIdAndDepartmentId(tenantUserId, departmentId)
                .map(TenantUserDepartmentMapper::toDomain);
    }

    @Override
    public Optional<TenantUserDepartment> findPrimaryByTenantUserId(Long tenantUserId) {
        return tenantUserDepartmentRepo.findByTenantUserIdAndIsPrimaryTrue(tenantUserId)
                .map(TenantUserDepartmentMapper::toDomain);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        tenantUserDepartmentRepo.deleteById(id);
    }
}
