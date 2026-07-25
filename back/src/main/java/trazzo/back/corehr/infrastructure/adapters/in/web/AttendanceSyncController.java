package trazzo.back.corehr.infrastructure.adapters.in.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import trazzo.back.corehr.application.dto.command.MarkAttendanceCommand;
import trazzo.back.corehr.application.dto.command.SyncAttendanceBatchItemCommand;
import trazzo.back.corehr.application.usecase.MarkAttendanceUseCase;
import trazzo.back.corehr.application.usecase.SyncAttendanceUseCase;
import trazzo.back.corehr.infrastructure.adapters.in.web.dto.BiometricIdentifyRequest;
import trazzo.back.corehr.infrastructure.adapters.in.web.dto.AttendanceResponse;
import trazzo.back.corehr.infrastructure.adapters.in.web.dto.MarcacionSyncResponse;

import java.util.List;

@RestController
@RequestMapping("/asistencia")
@RequiredArgsConstructor
public class AttendanceSyncController {

    private final MarkAttendanceUseCase markAttendanceUseCase;
    private final SyncAttendanceUseCase syncAttendanceUseCase;

    @PostMapping("/marcar")
    public ResponseEntity<AttendanceResponse> mark(@Valid @RequestBody BiometricIdentifyRequest request) {
        var command = new MarkAttendanceCommand(
                request.encryptedTemplateBase64(),
                request.encryptedAesKeyBase64(),
                request.ivBase64(),
                request.tagBase64(),
                request.capturedAtUtc(),
                request.deviceCode()
        );
        var result = markAttendanceUseCase.mark(command);
        return ResponseEntity.ok(AttendanceResponse.from(result));
    }

    @PostMapping("/sync")
    public ResponseEntity<MarcacionSyncResponse> sync(@Valid @RequestBody List<BiometricIdentifyRequest> requests) {
        var commands = requests.stream()
                .map(r -> new SyncAttendanceBatchItemCommand(
                        r.encryptedTemplateBase64(),
                        r.encryptedAesKeyBase64(),
                        r.ivBase64(),
                        r.tagBase64(),
                        r.capturedAtUtc(),
                        r.deviceCode(),
                        r.offlineEventId(),
                        r.retryCount()))
                .toList();

        Long tenantUserId = null;
        if (!requests.isEmpty() && requests.getFirst().tenantUserId() != null) {
            tenantUserId = requests.getFirst().tenantUserId();
        }

        var result = syncAttendanceUseCase.syncBatch(commands, tenantUserId);
        var response = new MarcacionSyncResponse(
                "Lote aceptado para procesamiento asincrónico.",
                result.acceptedCount(),
                result.correlationId());

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}
