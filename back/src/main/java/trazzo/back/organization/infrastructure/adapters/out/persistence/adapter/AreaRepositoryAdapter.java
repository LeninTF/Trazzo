package trazzo.back.organization.infrastructure.adapters.out.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import trazzo.back.organization.application.port.out.AreaRepositoryPort;
import trazzo.back.organization.domain.model.business.Area;
import trazzo.back.organization.infrastructure.adapters.out.persistence.mapper.OrgMapper;
import trazzo.back.organization.infrastructure.adapters.out.persistence.repository.AreaJpaRepository;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AreaRepositoryAdapter implements AreaRepositoryPort {

    private final AreaJpaRepository areaRepo;

    @Override
    public Area save(Area area) {
        return OrgMapper.toDomain(areaRepo.save(OrgMapper.toEntity(area)));
    }

    @Override
    public Optional<Area> findById(Long id) {
        return areaRepo.findById(id).map(OrgMapper::toDomain);
    }

    @Override
    public List<Area> findAll(Long branchId, Boolean state, String search, int page, int size, String sort) {
        var pageable = PageRequest.of(page, size, parseSort(sort));
        return areaRepo.findByFilters(branchId, state, blankToNull(search), pageable)
                .stream().map(OrgMapper::toDomain).toList();
    }

    @Override
    public long count(Long branchId, Boolean state, String search) {
        return areaRepo.findByFilters(branchId, state, blankToNull(search), PageRequest.of(0, 1))
                .getTotalElements();
    }

    @Override
    public boolean existsByBranchIdAndName(Long branchId, String name) {
        return areaRepo.existsByBranchIdAndName(branchId, name);
    }

    @Override
    public boolean existsByBranchIdAndNameAndIdNot(Long branchId, String name, Long id) {
        return areaRepo.existsByBranchIdAndNameAndIdNot(branchId, name, id);
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
