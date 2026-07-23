package trazzo.back.incidents.infrastructure.adapters.in.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import trazzo.back.incidents.application.dto.command.CreateIncidentCommand;
import trazzo.back.incidents.application.dto.command.IncidentStateChangeCommand;
import trazzo.back.incidents.application.dto.command.PatchIncidentCommand;
import trazzo.back.incidents.application.port.in.EvidenceUseCase;
import trazzo.back.incidents.application.port.in.IncidentUseCase;
import trazzo.back.incidents.application.port.in.NotificationUseCase;
import trazzo.back.incidents.infrastructure.adapters.in.web.dto.*;
import trazzo.back.corehr.application.port.out.TenantUserPort;
import trazzo.back.shared.application.port.out.FileStoragePort;
import trazzo.back.shared.security.AuthenticatedUser;

import java.time.LocalDate;

@RestController
@RequestMapping("/incidentes")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentUseCase incidentUseCase;
    private final EvidenceUseCase evidenceUseCase;
    private final NotificationUseCase notificationUseCase;
    private final FileStoragePort fileStoragePort;
    private final TenantUserPort tenantUserPort;

    private String buildPublicUrl(String fileKey) {
        return fileStoragePort.buildPublicUrl(fileKey);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('incidencias.ver-propias')")
    public ResponseEntity<IncidentListResponse> list(
            @RequestParam(required = false) String scope,
            @RequestParam(name = "sede_id", required = false) String sedeId,
            @RequestParam(name = "area_id", required = false) String areaId,
            @RequestParam(name = "departamento_id", required = false) String departamentoId,
            @RequestParam(required = false) String state,
            @RequestParam(name = "tipo_id", required = false) String tipoId,
            @RequestParam(required = false) LocalDate desde,
            @RequestParam(required = false) LocalDate hasta,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        String tenantUserId = null;
        if ("SELF".equals(scope) && user != null) {
            tenantUserId = tenantUserPort.findIdByMasterUserId(user.id())
                    .map(String::valueOf)
                    .orElse(null);
        }
        var result = incidentUseCase.findAll(tenantUserId, scope, sedeId, areaId, departamentoId,
                state, tipoId, desde, hasta, search, page, size, sort);
        return ResponseEntity.ok(IncidentListResponse.from(result, scope));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('incidencias.crear')")
    public ResponseEntity<IncidentResponse> create(
            @Valid @RequestBody CreateIncidentRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        var tenantUserId = tenantUserPort.findIdByMasterUserId(user.id())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró usuario de tenant para el usuario autenticado"));
        var command = new CreateIncidentCommand(
                String.valueOf(tenantUserId),
                request.incidenciaTypeId(),
                request.comment());
        var result = incidentUseCase.create(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(IncidentResponse.from(result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('incidencias.ver-propias')")
    public ResponseEntity<IncidentResponse> getById(@PathVariable String id) {
        return incidentUseCase.findById(id)
                .map(result -> ResponseEntity.ok(IncidentResponse.from(result)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<IncidentResponse> patch(
            @PathVariable String id,
            @Valid @RequestBody PatchIncidentRequest request
    ) {
        var command = new PatchIncidentCommand(request.comment());
        var result = incidentUseCase.patch(id, command);
        return ResponseEntity.ok(IncidentResponse.from(result));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAuthority('incidencias.aprobar-rechazar')")
    public ResponseEntity<IncidentResponse> changeState(
            @PathVariable String id,
            @Valid @RequestBody IncidentStateChangeRequest request
    ) {
        var command = new IncidentStateChangeCommand(request.state(), request.daysGranted(), request.motivoRechazo());
        var result = incidentUseCase.changeState(id, command);
        return ResponseEntity.ok(IncidentResponse.from(result));
    }

    @PostMapping("/{id}/evidencias")
    public ResponseEntity<IncidentEvidenceResponse> createEvidence(
            @PathVariable String id,
            @Valid @RequestBody CreateEvidenceRequest request
    ) {
        var command = new trazzo.back.incidents.application.dto.command.CreateEvidenceCommand(
                request.fileName(), request.fileKey(), request.mimeType(), request.fileSize());
        var result = evidenceUseCase.create(id, command);
        return ResponseEntity.status(HttpStatus.CREATED).body(IncidentEvidenceResponse.from(result));
    }

    @GetMapping("/{id}/evidencias")
    public ResponseEntity<java.util.List<IncidentEvidenceResponse>> listEvidences(@PathVariable String id) {
        var results = evidenceUseCase.findAllByIncidentId(id);
        var response = results.stream().map(IncidentEvidenceResponse::from).toList();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/evidencias/{evidenceId}")
    public ResponseEntity<Void> deleteEvidence(@PathVariable String id, @PathVariable String evidenceId) {
        evidenceUseCase.delete(id, evidenceId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/notificar")
    @PreAuthorize("hasAuthority('incidencias.aprobar-rechazar')")
    public ResponseEntity<Void> notify(@PathVariable String id, @Valid @RequestBody NotifyIncidentRequest request) {
        var command = new trazzo.back.incidents.application.dto.command.NotifyIncidentCommand(request.tipo());
        notificationUseCase.notify(id, command);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{id}/justificar")
    public ResponseEntity<Void> justify(@PathVariable String id) {
        notificationUseCase.justifyAttendance(id);
        return ResponseEntity.accepted().build();
    }
}
