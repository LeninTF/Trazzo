package trazzo.back.corehr.application.usecase;

import lombok.RequiredArgsConstructor;
import trazzo.back.corehr.application.dto.command.PatchAttendanceCommand;
import trazzo.back.corehr.application.dto.result.AttendanceResult;
import trazzo.back.corehr.application.dto.result.PaginatedResult;
import trazzo.back.corehr.application.port.in.AttendanceUseCase;
import trazzo.back.corehr.application.port.out.AttendanceRepositoryPort;
import trazzo.back.corehr.application.port.out.EventPublisherPort;
import trazzo.back.corehr.domain.model.AttendanceState;
import trazzo.back.corehr.domain.model.attendance.Attendance;

import java.time.LocalDate;
import java.util.Optional;

@RequiredArgsConstructor
public class AttendanceService implements AttendanceUseCase {

    private final AttendanceRepositoryPort attendanceRepository;
    private final EventPublisherPort eventPublisher;

    @Override
    public Optional<AttendanceResult> findById(String id) {
        return attendanceRepository.findById(id).map(this::toResult);
    }

    @Override
    public PaginatedResult<AttendanceResult> findAll(String scope, Long branchId, Long areaId, Long departamentoId,
                                                      LocalDate dateFrom, LocalDate dateTo, AttendanceState state,
                                                      Long tenantUserId, int page, int size, String sort) {
        var items = attendanceRepository.findAll(scope, branchId, areaId, departamentoId,
                dateFrom, dateTo, state, tenantUserId, page, size, sort);
        var total = attendanceRepository.count(scope, branchId, areaId, departamentoId,
                dateFrom, dateTo, state, tenantUserId);
        var results = items.stream().map(this::toResult).toList();
        var totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        return new PaginatedResult<>(results, page, size, total, totalPages);
    }

    @Override
    public AttendanceResult correct(String id, PatchAttendanceCommand command) {
        var attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Registro de asistencia no encontrado: " + id));
        if (command.checkIn() != null) {
            attendance.updateCheckIn(command.checkIn());
        }
        if (command.checkOut() != null) {
            attendance.updateCheckOut(command.checkOut());
        }
        if (command.state() != null || command.minutesLate() != null) {
            var newState = command.state() != null ? command.state() : attendance.getState();
            var newMinutes = command.minutesLate() != null ? command.minutesLate() : attendance.getMinutesLate();
            attendance.correct(newMinutes, newState);
        }
        var saved = attendanceRepository.save(attendance);
        var events = attendance.pullDomainEvents();
        events.forEach(eventPublisher::publish);
        return toResult(saved);
    }

    private AttendanceResult toResult(Attendance attendance) {
        return new AttendanceResult(
                attendance.getId(),
                attendance.getTenantUserId(),
                null,
                attendance.getScheduleId(),
                null,
                attendance.getDeviceId(),
                null,
                attendance.getCheckIn(),
                attendance.getCheckOut(),
                attendance.getAttendanceDate(),
                attendance.getMinutesLate(),
                attendance.getState(),
                attendance.getCreatedAt(),
                attendance.getUpdatedAt()
        );
    }
}
