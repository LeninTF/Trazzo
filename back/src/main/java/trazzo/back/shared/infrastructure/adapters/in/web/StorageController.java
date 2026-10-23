package trazzo.back.shared.infrastructure.adapters.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import trazzo.back.corehr.application.port.out.TenantUserPort;
import trazzo.back.incidents.application.port.in.IncidentUseCase;
import trazzo.back.incidents.domain.specification.IncidentEvidenceSpec;
import trazzo.back.shared.application.port.out.FileStoragePort;
import trazzo.back.shared.infrastructure.adapters.in.web.dto.PresignedUrlResponse;
import trazzo.back.shared.security.AuthenticatedUser;
import trazzo.back.shared.security.TenantAuthorization;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/storage")
public class StorageController {

    private final FileStoragePort fileStoragePort;
    private final TenantUserPort tenantUserPort;
    private final IncidentUseCase incidentUseCase;
    private final TenantAuthorization authz;

    public StorageController(FileStoragePort fileStoragePort, TenantUserPort tenantUserPort,
                             IncidentUseCase incidentUseCase, TenantAuthorization authz) {
        this.fileStoragePort = fileStoragePort;
        this.tenantUserPort = tenantUserPort;
        this.incidentUseCase = incidentUseCase;
        this.authz = authz;
    }

    // AUD-26: nombre normalizado, MIME de la lista blanca de evidencias e incidencia propia (o de un aprobador).
    @GetMapping("/presigned-url")
    @PreAuthorize("@authz.any('incidencias.crear')")
    public ResponseEntity<PresignedUrlResponse> getPresignedUrl(
            @RequestParam String fileName,
            @RequestParam String contentType,
            @RequestParam(name = "incident_id") Long incidentId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        String name = baseName(fileName);
        if (!new IncidentEvidenceSpec().isAllowedMimeType(contentType)) {
            throw new IllegalArgumentException("Tipo de archivo no permitido para una evidencia");
        }
        Long tenantUserId = Optional.ofNullable(user)
                .flatMap(u -> tenantUserPort.findIdByMasterUserId(u.id()))
                .orElseThrow(() -> new AccessDeniedException(
                        "No se encontró usuario de tenant para el usuario autenticado"));
        var incident = incidentUseCase.findById(incidentId.intValue())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incidencia no encontrada"));
        if (!authz.approver() && !Objects.equals(incident.tenantUserId(), tenantUserId.intValue())) {
            throw new AccessDeniedException("La incidencia pertenece a otro usuario");
        }

        String objectKey = "evidences/" + tenantUserId + "/" + incidentId + "/" + UUID.randomUUID() + "/" + name;
        String presignedUrl = fileStoragePort.generatePresignedPutUrl(objectKey, contentType, Duration.ofMinutes(15));

        return ResponseEntity.ok(new PresignedUrlResponse(presignedUrl, objectKey));
    }

    // Solo el nombre: "../../x.pdf" o "C:\fakepath\x.pdf" no pueden salir de su carpeta en el bucket.
    private static String baseName(String fileName) {
        String name = fileName.substring(Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\')) + 1).trim();
        if (name.isEmpty() || ".".equals(name) || "..".equals(name)) {
            throw new IllegalArgumentException("Nombre de archivo no válido");
        }
        return name;
    }
}
