package trazzo.back.organization.infrastructure.adapters.out.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import trazzo.back.organization.application.port.out.RoleRepositoryPort;
import trazzo.back.organization.domain.model.roles.Role;
import trazzo.back.organization.infrastructure.adapters.out.persistence.mapper.OrgMapper;
import trazzo.back.organization.infrastructure.adapters.out.persistence.repository.RoleJpaRepository;

import java.util.List;
import java.util.Optional;

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
        return roleRepo.findById(id).map(OrgMapper::toDomain);
    }

    @Override
    public List<Role> findAll(String search, int page, int size, String sort) {
        var pageable = PageRequest.of(page, size, parseSort(sort));
        return roleRepo.findByFilters(blankToNull(search), pageable)
                .stream().map(OrgMapper::toDomain).toList();
    }

    @Override
    public long count(String search) {
        return roleRepo.findByFilters(blankToNull(search), PageRequest.of(0, 1)).getTotalElements();
    }

    @Override
    public boolean existsByName(String name) {
        return roleRepo.existsByName(name);
    }

    @Override
    public boolean existsByNameAndIdNot(String name, String id) {
        return roleRepo.existsByNameAndIdNot(name, id);
    }

    @Override
    public void deleteById(String id) {
        roleRepo.deleteById(id);
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) return Sort.by(Sort.Direction.ASC, "name");
        var parts = sort.split(",");
        var direction = parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim())
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(direction, mapField(parts[0].trim()));
    }

    private String mapField(String field) {
        return switch (field) {
            case "created_at", "createdAt" -> "createdAt";
            case "updated_at", "updatedAt" -> "updatedAt";
            default -> "name";
        };
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
