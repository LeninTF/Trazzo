package trazzo.back.shared.infrastructure.adapters.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import trazzo.back.corehr.application.port.out.TenantUserPort;
import trazzo.back.shared.application.port.out.FileStoragePort;
import trazzo.back.shared.infrastructure.adapters.in.web.dto.PresignedUrlResponse;
import trazzo.back.shared.security.AuthenticatedUser;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/storage")
public class StorageController {

    private final FileStoragePort fileStoragePort;
    private final TenantUserPort tenantUserPort;

    public StorageController(FileStoragePort fileStoragePort, TenantUserPort tenantUserPort) {
        this.fileStoragePort = fileStoragePort;
        this.tenantUserPort = tenantUserPort;
    }

    @GetMapping("/presigned-url")
    @PreAuthorize("hasAuthority('incidencias.crear')")
    public ResponseEntity<PresignedUrlResponse> getPresignedUrl(
            @RequestParam String fileName,
            @RequestParam String contentType,
            @RequestParam(name = "incident_id", required = false) Long incidentId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        String tenantId = Optional.ofNullable(user)
                .flatMap(u -> tenantUserPort.findIdByMasterUserId(u.id()))
                .map(String::valueOf)
                .orElse("unknown-tenant");

        StringBuilder objectKeyBuilder = new StringBuilder("evidences/").append(tenantId).append("/");
        if (incidentId != null) {
            objectKeyBuilder.append(incidentId).append("/");
        }
        objectKeyBuilder.append(UUID.randomUUID()).append("/").append(fileName);
        String objectKey = objectKeyBuilder.toString();

        String presignedUrl = fileStoragePort.generatePresignedPutUrl(objectKey, contentType, Duration.ofMinutes(15));

        return ResponseEntity.ok(new PresignedUrlResponse(presignedUrl, objectKey));
    }
}
