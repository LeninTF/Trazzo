package trazzo.back.corehr.application.usecase;

import lombok.RequiredArgsConstructor;
import trazzo.back.corehr.application.dto.command.CreateUserScheduleCommand;
import trazzo.back.corehr.application.dto.result.PaginatedResult;
import trazzo.back.corehr.application.dto.result.ShiftResult;
import trazzo.back.corehr.application.dto.result.UserScheduleResult;
import trazzo.back.corehr.application.port.in.UserScheduleUseCase;
import trazzo.back.corehr.application.port.out.ScheduleRepositoryPort;
import trazzo.back.corehr.application.port.out.TenantUserPort;
import trazzo.back.corehr.application.port.out.UserScheduleRepositoryPort;
import trazzo.back.corehr.domain.model.TenantUserState;
import trazzo.back.corehr.domain.model.schedule.UserSchedule;

@RequiredArgsConstructor
public class UserScheduleService implements UserScheduleUseCase {

    private final UserScheduleRepositoryPort userScheduleRepository;
    private final ScheduleRepositoryPort scheduleRepository;
    private final TenantUserPort tenantUserPort;

    @Override
    public UserScheduleResult create(CreateUserScheduleCommand command) {
        if (!tenantUserPort.existsById(command.tenantUserId())) {
            throw new IllegalArgumentException("TenantUser no encontrado: " + command.tenantUserId());
        }
        var state = tenantUserPort.findStateById(command.tenantUserId());
        if (state.isPresent() && state.get() != TenantUserState.ACTIVO) {
            throw new IllegalStateException("El trabajador no está ACTIVO");
        }
        if (!scheduleRepository.findById(command.scheduleId()).isPresent()) {
            throw new IllegalArgumentException("Schedule no encontrado: " + command.scheduleId());
        }
        var userSchedule = UserSchedule.create(
                command.tenantUserId(), command.scheduleId(), command.description(),
                command.entryTime(), command.departureTime()
        );
        var saved = userScheduleRepository.save(userSchedule);
        return toResult(saved);
    }

    @Override
    public PaginatedResult<UserScheduleResult> findAll(Long tenantUserId, Long scheduleId, int page, int size) {
        var items = userScheduleRepository.findAll(tenantUserId, scheduleId, page, size);
        var total = userScheduleRepository.count(tenantUserId, scheduleId);
        var results = items.stream().map(this::toResult).toList();
        var totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        return new PaginatedResult<>(results, page, size, total, totalPages);
    }

    @Override
    public void deleteById(Long id) {
        if (!userScheduleRepository.findById(id).isPresent()) {
            throw new IllegalArgumentException("Asignación no encontrada: " + id);
        }
        userScheduleRepository.deleteById(id);
    }

    private UserScheduleResult toResult(UserSchedule us) {
        var scheduleSummary = scheduleRepository.findById(us.getScheduleId())
                .map(s -> new ShiftResult.ScheduleSummary(s.getId(), s.getName()))
                .orElse(null);
        return new UserScheduleResult(
                us.getId(),
                us.getTenantUserId(),
                us.getScheduleId(),
                scheduleSummary,
                us.getDescription(),
                us.getEntryTime(),
                us.getDepartureTime(),
                us.getCreatedAt(),
                us.getUpdatedAt()
        );
    }
}
