package trazzo.back.corehr.application.port.in;

import trazzo.back.corehr.application.dto.command.CreateShiftCommand;
import trazzo.back.corehr.application.dto.command.PatchShiftCommand;
import trazzo.back.corehr.application.dto.result.PaginatedResult;
import trazzo.back.corehr.application.dto.result.ShiftResult;

import java.util.Optional;

public interface ShiftUseCase {
    ShiftResult create(CreateShiftCommand command);
    Optional<ShiftResult> findById(Long id);
    PaginatedResult<ShiftResult> findAll(String search, int page, int size, String sort);
    ShiftResult patch(Long id, PatchShiftCommand command);
    void deleteById(Long id);
}
