package trazzo.back.corehr.application.usecase;

import lombok.RequiredArgsConstructor;
import trazzo.back.corehr.application.dto.command.CreateUserScheduleCommand;
import trazzo.back.corehr.application.dto.command.BulkAssignSchedulesCommand;
import trazzo.back.corehr.application.dto.command.BulkAssignSchedulesCommand.Item;
import trazzo.back.corehr.application.dto.result.BulkAssignSchedulesResult;
import trazzo.back.corehr.application.dto.result.BulkAssignSchedulesResult.ItemResult;
import trazzo.back.corehr.application.dto.result.PaginatedResult;
import trazzo.back.corehr.application.dto.result.ShiftResult;
import trazzo.back.corehr.application.dto.result.UserScheduleResult;
import trazzo.back.corehr.application.port.in.UserScheduleUseCase;
import trazzo.back.corehr.application.port.out.AuditScheduleAssignmentPort;
import trazzo.back.corehr.application.port.out.ScheduleRepositoryPort;
import trazzo.back.corehr.application.port.out.TenantUserPort;
import trazzo.back.corehr.application.port.out.UserScheduleRepositoryPort;
import trazzo.back.corehr.domain.exception.ScheduleConflictException;
import trazzo.back.corehr.domain.model.TenantUserState;
import trazzo.back.corehr.domain.model.schedule.Schedule;
import trazzo.back.corehr.domain.model.schedule.UserSchedule;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@RequiredArgsConstructor
public class UserScheduleService implements UserScheduleUseCase {

    public static final int BULK_MAX_SIZE = 500;

    private final UserScheduleRepositoryPort userScheduleRepository;
    private final ScheduleRepositoryPort scheduleRepository;
    private final TenantUserPort tenantUserPort;
    private final AuditScheduleAssignmentPort auditPort;

    @Override
    public UserScheduleResult create(CreateUserScheduleCommand command) {
        if (!tenantUserPort.existsById(command.tenantUserId())) {
            throw new IllegalArgumentException("TenantUser no encontrado: " + command.tenantUserId());
        }
        var state = tenantUserPort.findStateById(command.tenantUserId());
        if (state.isPresent() && state.get() != TenantUserState.ACTIVO) {
            throw new IllegalStateException("El trabajador no está ACTIVO");
        }
        var schedule = scheduleRepository.findById(command.scheduleId())
                .orElseThrow(() -> new IllegalArgumentException("Schedule no encontrado: " + command.scheduleId()));
        if (userScheduleRepository.existsByTenantUserIdAndScheduleId(command.tenantUserId(), command.scheduleId())) {
            throw new ScheduleConflictException(
                    "El trabajador ya tiene asignado el schedule " + command.scheduleId());
        }
        var userSchedule = UserSchedule.create(
                command.tenantUserId(), command.scheduleId(), command.description(),
                schedule.getEntryTime(), schedule.getDepartureTime()
        );
        var saved = userScheduleRepository.save(userSchedule);
        auditPort.recordAssignment(saved.getTenantUserId(), saved.getScheduleId(), saved.getId(),
                AuditScheduleAssignmentPort.Action.CREATE);
        return toResult(saved);
    }

    @Override
    public BulkAssignSchedulesResult bulkAssign(BulkAssignSchedulesCommand command) {
        if (command.tenantUserIds() == null || command.tenantUserIds().isEmpty()) {
            throw new IllegalArgumentException("tenantUserIds no puede ser vacío");
        }
        if (command.tenantUserIds().size() > BULK_MAX_SIZE) {
            throw new IllegalArgumentException(
                    "El lote excede el máximo permitido de " + BULK_MAX_SIZE + " usuarios por solicitud");
        }
        var dedupedIds = new ArrayList<>(new HashSet<>(command.tenantUserIds()));
        var schedule = scheduleRepository.findById(command.scheduleId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Schedule no encontrado: " + command.scheduleId()));

        var results = new ArrayList<ItemResult>(dedupedIds.size());
        var toCreate = new ArrayList<Item>(dedupedIds.size());
        var existingAssignments = new HashSet<Long>();
        for (var id : dedupedIds) {
            if (userScheduleRepository.existsByTenantUserIdAndScheduleId(id, command.scheduleId())) {
                existingAssignments.add(id);
            }
        }
        for (var id : dedupedIds) {
            if (existingAssignments.contains(id)) {
                results.add(new ItemResult(id, "skipped", "El trabajador ya tiene asignado el schedule"));
                continue;
            }
            if (!tenantUserPort.existsById(id)) {
                results.add(new ItemResult(id, "error", "TenantUser no encontrado"));
                continue;
            }
            var state = tenantUserPort.findStateById(id);
            if (state.isPresent() && state.get() != TenantUserState.ACTIVO) {
                results.add(new ItemResult(id, "error", "El trabajador no está ACTIVO"));
                continue;
            }
            toCreate.add(new Item(id, command.description(),
                    schedule.getEntryTime(), schedule.getDepartureTime()));
        }

        var created = userScheduleRepository.bulkCreate(command.scheduleId(), toCreate);
        for (var us : created) {
            results.add(new ItemResult(us.getTenantUserId(), "created", null));
            auditPort.recordAssignment(us.getTenantUserId(), us.getScheduleId(), us.getId(),
                    AuditScheduleAssignmentPort.Action.CREATE);
        }
        return new BulkAssignSchedulesResult(
                command.scheduleId(),
                results,
                results.size(),
                (int) results.stream().filter(r -> "created".equals(r.status())).count(),
                (int) results.stream().filter(r -> "skipped".equals(r.status())).count(),
                (int) results.stream().filter(r -> "error".equals(r.status())).count()
        );
    }

    @Override
    public PaginatedResult<UserScheduleResult> findAll(
            Long tenantUserId, Long scheduleId, Long shiftId, int page, int size
    ) {
        var items = userScheduleRepository.findAll(tenantUserId, scheduleId, shiftId, page, size);
        var total = userScheduleRepository.count(tenantUserId, scheduleId, shiftId);
        var tenantUserIds = items.stream().map(UserSchedule::getTenantUserId).distinct().toList();
        var orgBundles = tenantUserIds.isEmpty()
                ? java.util.Collections.<Long, TenantUserPort.OrgAssignmentBundle>emptyMap()
                : tenantUserPort.findOrgAssignmentsByUserIds(tenantUserIds);
        var basicInfos = new java.util.HashMap<Long, TenantUserPort.TenantUserBasicInfo>(tenantUserIds.size());
        for (var id : tenantUserIds) {
            tenantUserPort.findBasicInfoById(id).ifPresent(b -> basicInfos.put(id, b));
        }
        var results = items.stream().map(us -> toResult(us,
                basicInfos.get(us.getTenantUserId()),
                orgBundles.get(us.getTenantUserId()))).toList();
        var totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        return new PaginatedResult<>(results, page, size, total, totalPages);
    }

    @Override
    public List<UserScheduleResult> findByTenantUserId(Long tenantUserId) {
        return userScheduleRepository.findByTenantUserId(tenantUserId).stream()
                .map(this::toResult).toList();
    }

    @Override
    public Optional<UserScheduleResult> findById(Long id) {
        return userScheduleRepository.findById(id).map(this::toResult);
    }

    @Override
    public void deleteById(Long id) {
        var existing = userScheduleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Asignación no encontrada: " + id));
        userScheduleRepository.deleteById(id);
        auditPort.recordAssignment(existing.getTenantUserId(), existing.getScheduleId(), existing.getId(),
                AuditScheduleAssignmentPort.Action.DELETE);
    }

    private UserScheduleResult toResult(UserSchedule us) {
        return toResult(us, null, null);
    }

    private UserScheduleResult toResult(UserSchedule us,
                                        TenantUserPort.TenantUserBasicInfo basic,
                                        TenantUserPort.OrgAssignmentBundle org) {
        var scheduleSummary = scheduleRepository.findById(us.getScheduleId())
                .map(s -> new ShiftResult.ScheduleSummary(
                        s.getId(), s.getName(), s.getEntryTime(), s.getDepartureTime(), s.getDaysOfWeek()))
                .orElse(null);
        UserScheduleResult.TenantUserSummary tenantUserSummary = null;
        if (basic != null) {
            String sede = org != null && !org.sedes().isEmpty() ? org.sedes().get(0).nombre() : null;
            String area = org != null && !org.areas().isEmpty() ? org.areas().get(0).nombre() : null;
            String department = org != null && !org.departamentos().isEmpty() ? org.departamentos().get(0).nombre() : null;
            tenantUserSummary = new UserScheduleResult.TenantUserSummary(
                    basic.id(), basic.nombre(), basic.apellidoPaterno(), basic.apellidoMaterno(),
                    sede, area, department);
        }
        return new UserScheduleResult(
                us.getId(),
                us.getTenantUserId(),
                us.getScheduleId(),
                scheduleSummary,
                us.getDescription(),
                us.getEntryTime(),
                us.getDepartureTime(),
                us.getCreatedAt(),
                us.getUpdatedAt(),
                tenantUserSummary
        );
    }
}
