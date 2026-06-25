package trazzo.back.incidents.application.usecase;

import lombok.RequiredArgsConstructor;
import trazzo.back.incidents.application.dto.command.CreateIncidentCommand;
import trazzo.back.incidents.application.dto.command.IncidentStateChangeCommand;
import trazzo.back.incidents.application.dto.command.PatchIncidentCommand;
import trazzo.back.incidents.application.dto.result.*;
import trazzo.back.incidents.application.port.in.IncidentUseCase;
import trazzo.back.incidents.application.port.out.EventPublisherPort;
import trazzo.back.incidents.application.port.out.IncidentRepositoryPort;
import trazzo.back.incidents.application.port.out.IncidentTypeRepositoryPort;
import trazzo.back.incidents.application.port.out.TenantUserPort;
import trazzo.back.incidents.domain.model.Incident;
import trazzo.back.incidents.domain.model.IncidentState;
import trazzo.back.incidents.domain.model.IncidentType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class IncidentService implements IncidentUseCase {

    private final IncidentRepositoryPort incidentRepository;
    private final IncidentTypeRepositoryPort typeRepository;
    private final TenantUserPort tenantUserPort;
    private final EventPublisherPort eventPublisher;

    @Override
    public IncidentResult create(CreateIncidentCommand command) {
        var type = typeRepository.findById(command.incidentTypeId())
                .orElseThrow(() -> new IllegalArgumentException("Tipo de incidencia no encontrado: " + command.incidentTypeId()));

        if (!type.isActivo()) {
            throw new IllegalStateException("El tipo de incidencia no está activo");
        }

        var incident = Incident.create(command.tenantUserId(), command.incidentTypeId(), command.comment());
        incident.attachType(type);

        var saved = incidentRepository.save(incident);
        publishEvents(saved);

        return toResult(saved);
    }

    @Override
    public Optional<IncidentResult> findById(String id) {
        return incidentRepository.findById(id).map(this::toResult);
    }

    @Override
    public PaginatedResult<IncidentResult> findAll(String scope, String sedeId, String areaId,
                                                    String departamentoId, String state, String tipoId,
                                                    LocalDate desde, LocalDate hasta, String search,
                                                    int page, int size, String sort) {

        LocalDateTime desdeDt = desde != null ? desde.atStartOfDay() : null;
        LocalDateTime hastaDt = hasta != null ? hasta.plusDays(1).atStartOfDay() : null;

        var incidents = incidentRepository.findAll(null, state, tipoId, desdeDt, hastaDt, search, page, size, sort);
        var total = incidentRepository.count(null, state, tipoId, desdeDt, hastaDt, search);

        var results = incidents.stream().map(this::toResult).toList();
        var totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        return new PaginatedResult<>(results, page, size, total, totalPages);
    }

    @Override
    public IncidentResult patch(String id, PatchIncidentCommand command) {
        var incident = incidentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Incidencia no encontrada: " + id));

        incident.updateComment(command.comment());
        var saved = incidentRepository.save(incident);
        return toResult(saved);
    }

    @Override
    public IncidentResult changeState(String id, IncidentStateChangeCommand command) {
        var incident = incidentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Incidencia no encontrada: " + id));

        if (command.state() == IncidentState.APROBADO) {
            if (command.daysGranted() != null && command.daysGranted() > 0) {
                var startDate = LocalDate.now();
                var endDate = startDate.plusDays(command.daysGranted() - 1);
                incident.approveWithPermission(startDate, endDate, command.daysGranted());
            } else {
                incident.approve();
            }
        } else if (command.state() == IncidentState.DENEGADO) {
            incident.deny(command.motivoRechazo());
        } else {
            throw new IllegalArgumentException("Estado no válido: " + command.state());
        }

        var saved = incidentRepository.save(incident);
        publishEvents(saved);
        return toResult(saved);
    }

    private void publishEvents(Incident incident) {
        var events = incident.pullDomainEvents();
        events.forEach(eventPublisher::publish);
    }

    private IncidentResult toResult(Incident incident) {
        IncidentTypeResult tipoResult = null;
        if (incident.getType() != null) {
            var t = incident.getType();
            tipoResult = new IncidentTypeResult(t.getId(), t.getNombre(), t.getDescripcion(),
                    t.isActivo(), t.getCreatedAt(), t.getUpdatedAt());
        }

        IncidentPermissionResult permisoResult = null;
        if (incident.getPermission() != null) {
            var p = incident.getPermission();
            permisoResult = new IncidentPermissionResult(p.getId(), p.getIncidentId(),
                    p.getStartDate(), p.getEndDate(), p.getDaysGranted(),
                    p.getCreatedAt(), p.getUpdatedAt());
        }

        List<IncidentEvidenceResult> evidenciasResult = incident.getEvidences().stream()
                .filter(e -> !e.isDeleted())
                .map(e -> new IncidentEvidenceResult(e.getId(), e.getIncidentId(),
                        e.getFileName(), e.getFileUrl(), e.getMimeType(), e.getFileSize(),
                        e.getCreatedAt(), e.getUpdatedAt()))
                .toList();

        IncidentResult.TenantUserBasicInfoResult tenantUserResult = null;
        if (incident.getTenantUserId() != null) {
            var optInfo = tenantUserPort.findBasicInfoById(incident.getTenantUserId());
            if (optInfo.isPresent()) {
                var info = optInfo.get();
                tenantUserResult = new IncidentResult.TenantUserBasicInfoResult(
                        info.id(), info.nombre(), info.apellidoPaterno(),
                        info.apellidoMaterno(), info.email());
            }
        }

        return new IncidentResult(
                incident.getId(),
                incident.getTenantUserId(),
                incident.getIncidentTypeId(),
                incident.getState(),
                incident.getComment(),
                incident.getRejectionReason(),
                tipoResult,
                permisoResult,
                evidenciasResult,
                tenantUserResult,
                incident.getCreatedAt(),
                incident.getUpdatedAt()
        );
    }
}
