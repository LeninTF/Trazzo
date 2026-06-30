package trazzo.back.organization.application.port.in;

import trazzo.back.organization.application.dto.command.CreateAreaCommand;
import trazzo.back.organization.application.dto.command.UpdateAreaCommand;
import trazzo.back.organization.application.dto.result.AreaResult;
import trazzo.back.organization.application.dto.result.PaginatedResult;

import java.util.Optional;

public interface AreaUseCase {
    AreaResult create(CreateAreaCommand command);
    Optional<AreaResult> findById(Long id);
    PaginatedResult<AreaResult> findAll(Long branchId, Boolean state, String search, int page, int size, String sort);
    AreaResult update(Long id, UpdateAreaCommand command);
    void delete(Long id);
}
