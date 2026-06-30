package trazzo.back.organization.application.usecase;

import lombok.RequiredArgsConstructor;
import trazzo.back.organization.application.dto.command.CreateBranchCommand;
import trazzo.back.organization.application.dto.command.UpdateBranchCommand;
import trazzo.back.organization.application.dto.result.BranchResult;
import trazzo.back.organization.application.dto.result.PaginatedResult;
import trazzo.back.organization.application.port.in.BranchUseCase;
import trazzo.back.organization.application.port.out.BranchRepositoryPort;
import trazzo.back.organization.domain.exception.DuplicateOrgNameException;
import trazzo.back.organization.domain.exception.OrgNotFoundException;
import trazzo.back.organization.domain.model.business.Branch;

import java.util.Optional;

@RequiredArgsConstructor
public class BranchService implements BranchUseCase {

    private final BranchRepositoryPort branchRepository;

    @Override
    public BranchResult create(CreateBranchCommand command) {
        if (branchRepository.existsByName(command.name())) {
            throw new DuplicateOrgNameException("Branch with name '" + command.name() + "' already exists");
        }
        var branch = Branch.create(command.name(), command.description());
        return toResult(branchRepository.save(branch));
    }

    @Override
    public Optional<BranchResult> findById(Long id) {
        return branchRepository.findById(id).map(this::toResult);
    }

    @Override
    public PaginatedResult<BranchResult> findAll(Boolean state, String search, int page, int size, String sort) {
        var items = branchRepository.findAll(state, search, page, size, sort);
        var total = branchRepository.count(state, search);
        return PaginatedResult.of(items.stream().map(this::toResult).toList(), page, size, total);
    }

    @Override
    public BranchResult update(Long id, UpdateBranchCommand command) {
        var branch = branchRepository.findById(id)
                .orElseThrow(() -> new OrgNotFoundException("Branch not found: " + id));
        if (branchRepository.existsByNameAndIdNot(command.name(), id)) {
            throw new DuplicateOrgNameException("Branch with name '" + command.name() + "' already exists");
        }
        branch.update(command.name(), command.description());
        return toResult(branchRepository.save(branch));
    }

    @Override
    public void delete(Long id) {
        var branch = branchRepository.findById(id)
                .orElseThrow(() -> new OrgNotFoundException("Branch not found: " + id));
        branch.softDelete();
        branchRepository.save(branch);
    }

    private BranchResult toResult(Branch branch) {
        return new BranchResult(
                branch.getId(), branch.getName(), branch.getDescription(),
                branch.isState(), branch.getCreatedAt(), branch.getUpdatedAt(), branch.getDeletedAt()
        );
    }
}
