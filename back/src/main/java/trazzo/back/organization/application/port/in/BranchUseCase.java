package trazzo.back.organization.application.port.in;

import trazzo.back.organization.application.dto.command.CreateBranchCommand;
import trazzo.back.organization.application.dto.command.UpdateBranchCommand;
import trazzo.back.organization.application.dto.result.BranchResult;
import trazzo.back.organization.application.dto.result.PaginatedResult;

import java.util.Optional;

public interface BranchUseCase {
    BranchResult create(CreateBranchCommand command);
    Optional<BranchResult> findById(Long id);
    PaginatedResult<BranchResult> findAll(Boolean state, String search, int page, int size, String sort);
    BranchResult update(Long id, UpdateBranchCommand command);
    void delete(Long id);
}
