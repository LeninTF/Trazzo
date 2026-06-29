package trazzo.back.corehr.application.port.out;

import trazzo.back.corehr.domain.model.AttendanceState;
import trazzo.back.corehr.domain.model.attendance.Attendance;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepositoryPort {
    Attendance save(Attendance attendance);
    Optional<Attendance> findById(String id);
    List<Attendance> findAll(String scope, Long branchId, Long areaId, Long departamentoId,
                             LocalDate dateFrom, LocalDate dateTo, AttendanceState state,
                             Long tenantUserId, int page, int size, String sort);
    long count(String scope, Long branchId, Long areaId, Long departamentoId,
               LocalDate dateFrom, LocalDate dateTo, AttendanceState state,
               Long tenantUserId);
}
