package trazzo.back.organization.infrastructure.adapters.out.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import trazzo.back.organization.application.port.out.RoleRepositoryPort;
import trazzo.back.organization.domain.model.roles.Role;
import trazzo.back.organization.infrastructure.adapters.out.persistence.OrgPersistenceUtils;
import trazzo.back.organization.infrastructure.adapters.out.persistence.mapper.OrgMapper;
import trazzo.back.organization.infrastructure.adapters.out.persistence.repository.RoleJpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RoleRepositoryAdapter implements RoleRepositoryPort {

    private final RoleJpaRepository roleRepo;

    @Override
    public Role save(Role role) {
        return OrgMapper.toDomain(roleRepo.save(OrgMapper.toEntity(role)));
    }

    @Override
    public Optional<Role> findById(String id) {
        return roleRepo.findById(UUID.fromString(id)).map(OrgMapper::toDomain);
    }

    @Override
    public List<Role> findAll(String search, int page, int size, String sort) {
        var pageable = PageRequest.of(page, size, OrgPersistenceUtils.parseSort(sort));
        return roleRepo.findByFilters(OrgPersistenceUtils.blankToNull(search), pageable)
                .stream().map(OrgMapper::toDomain).toList();
    }

    @Override
    public long count(String search) {
        return roleRepo.findByFilters(OrgPersistenceUtils.blankToNull(search), PageRequest.of(0, 1)).getTotalElements();
    }

    @Override
    public boolean existsByName(String name) {
        return roleRepo.existsByName(name);
    }

    @Override
    public boolean existsByNameAndIdNot(String name, String id) {
        return roleRepo.existsByNameAndIdNot(name, UUID.fromString(id));
    }

    @Override
    public void deleteById(String id) {
        roleRepo.deleteById(UUID.fromString(id));
    }
}
