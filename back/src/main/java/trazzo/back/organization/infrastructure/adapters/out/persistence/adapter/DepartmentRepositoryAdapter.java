package trazzo.back.organization.infrastructure.adapters.out.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import trazzo.back.organization.application.port.out.DepartmentRepositoryPort;
import trazzo.back.organization.domain.model.business.Department;
import trazzo.back.organization.infrastructure.adapters.out.persistence.OrgPersistenceUtils;
import trazzo.back.organization.infrastructure.adapters.out.persistence.mapper.OrgMapper;
import trazzo.back.organization.infrastructure.adapters.out.persistence.repository.DepartmentJpaRepository;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DepartmentRepositoryAdapter implements DepartmentRepositoryPort {

    private final DepartmentJpaRepository departmentRepo;

    @Override
    public Department save(Department department) {
        return OrgMapper.toDomain(departmentRepo.save(OrgMapper.toEntity(department)));
    }

    @Override
    public Optional<Department> findById(Long id) {
        return departmentRepo.findById(id).map(OrgMapper::toDomain);
    }

    @Override
    public List<Department> findAll(Long areaId, Boolean state, String search, int page, int size, String sort) {
        var pageable = PageRequest.of(page, size, OrgPersistenceUtils.parseSort(sort));
        return departmentRepo.findByFilters(areaId, state, OrgPersistenceUtils.blankToNull(search), pageable)
                .stream().map(OrgMapper::toDomain).toList();
    }

    @Override
    public long count(Long areaId, Boolean state, String search) {
        return departmentRepo.findByFilters(areaId, state, OrgPersistenceUtils.blankToNull(search), PageRequest.of(0, 1))
                .getTotalElements();
    }

    @Override
    public boolean existsByAreaIdAndName(Long areaId, String name) {
        return departmentRepo.existsByAreaIdAndName(areaId, name);
    }

    @Override
    public boolean existsByAreaIdAndNameAndIdNot(Long areaId, String name, Long id) {
        return departmentRepo.existsByAreaIdAndNameAndIdNot(areaId, name, id);
    }
}
