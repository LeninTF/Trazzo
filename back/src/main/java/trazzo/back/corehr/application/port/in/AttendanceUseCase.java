package trazzo.back.corehr.application.port.in;

import trazzo.back.corehr.application.dto.command.PatchAttendanceCommand;
import trazzo.back.corehr.application.dto.result.AttendanceResult;
import trazzo.back.corehr.application.dto.result.PaginatedResult;
import trazzo.back.corehr.domain.model.AttendanceState;

import java.time.LocalDate;
import java.util.Optional;

public interface AttendanceUseCase {
    Optional<AttendanceResult> findById(String id);
    PaginatedResult<AttendanceResult> findAll(String scope, Long branchId, Long areaId, Long departamentoId,
                                               LocalDate dateFrom, LocalDate dateTo, AttendanceState state,
                                               Long tenantUserId, int page, int size, String sort);
    AttendanceResult correct(String id, PatchAttendanceCommand command);
}
