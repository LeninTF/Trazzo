package trazzo.back.organization.infrastructure.adapters.out.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import trazzo.back.organization.application.port.out.PermissionRepositoryPort;
import trazzo.back.organization.domain.model.roles.Permissions;
import trazzo.back.organization.infrastructure.adapters.out.persistence.OrgPersistenceUtils;
import trazzo.back.organization.infrastructure.adapters.out.persistence.mapper.OrgMapper;
import trazzo.back.organization.infrastructure.adapters.out.persistence.repository.PermissionJpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PermissionRepositoryAdapter implements PermissionRepositoryPort {

    private final PermissionJpaRepository permissionRepo;

    @Override
    public Permissions save(Permissions permission) {
        return OrgMapper.toDomain(permissionRepo.save(OrgMapper.toEntity(permission)));
    }

    @Override
    public Optional<Permissions> findById(String id) {
        return permissionRepo.findById(UUID.fromString(id)).map(OrgMapper::toDomain);
    }

    @Override
    public List<Permissions> findAll(String search, int page, int size, String sort) {
        var pageable = PageRequest.of(page, size, OrgPersistenceUtils.parseSort(sort));
        return permissionRepo.findByFilters(OrgPersistenceUtils.likePattern(search), pageable)
                .stream().map(OrgMapper::toDomain).toList();
    }

    @Override
    public long count(String search) {
        return permissionRepo.findByFilters(OrgPersistenceUtils.likePattern(search), PageRequest.of(0, 1)).getTotalElements();
    }

    @Override
    public boolean existsByName(String name) {
        return permissionRepo.existsByName(name);
    }

    @Override
    public boolean existsByNameAndIdNot(String name, String id) {
        return permissionRepo.existsByNameAndIdNot(name, UUID.fromString(id));
    }

    @Override
    public void deleteById(String id) {
        permissionRepo.deleteById(UUID.fromString(id));
    }
}
