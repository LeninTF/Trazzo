package trazzo.back.organization.infrastructure.adapters.out.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import trazzo.back.organization.application.port.out.BranchRepositoryPort;
import trazzo.back.organization.domain.model.business.Branch;
import trazzo.back.organization.infrastructure.adapters.out.persistence.OrgPersistenceUtils;
import trazzo.back.organization.infrastructure.adapters.out.persistence.mapper.OrgMapper;
import trazzo.back.organization.infrastructure.adapters.out.persistence.repository.BranchJpaRepository;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class BranchRepositoryAdapter implements BranchRepositoryPort {

    private final BranchJpaRepository branchRepo;

    @Override
    public Branch save(Branch branch) {
        return OrgMapper.toDomain(branchRepo.save(OrgMapper.toEntity(branch)));
    }

    @Override
    public Optional<Branch> findById(Long id) {
        return branchRepo.findById(id).map(OrgMapper::toDomain);
    }

    @Override
    public List<Branch> findAll(Boolean state, String search, int page, int size, String sort) {
        var pageable = PageRequest.of(page, size, OrgPersistenceUtils.parseSort(sort));
        return branchRepo.findByFilters(state, OrgPersistenceUtils.blankToNull(search), pageable)
                .stream().map(OrgMapper::toDomain).toList();
    }

    @Override
    public long count(Boolean state, String search) {
        return branchRepo.findByFilters(state, OrgPersistenceUtils.blankToNull(search), PageRequest.of(0, 1))
                .getTotalElements();
    }

    @Override
    public boolean existsByName(String name) {
        return branchRepo.existsByName(name);
    }

    @Override
    public boolean existsByNameAndIdNot(String name, Long id) {
        return branchRepo.existsByNameAndIdNot(name, id);
    }
}
