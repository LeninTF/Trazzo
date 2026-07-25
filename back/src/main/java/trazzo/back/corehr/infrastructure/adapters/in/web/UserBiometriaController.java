package trazzo.back.corehr.infrastructure.adapters.in.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import trazzo.back.corehr.application.port.in.UserBiometriaUseCase;
import trazzo.back.corehr.infrastructure.adapters.in.web.dto.*;
import trazzo.back.corehr.infrastructure.adapters.out.enroll.EnrollService;

@RestController
@RequestMapping("/corehr/biometria")
@RequiredArgsConstructor
public class UserBiometriaController {

    private final UserBiometriaUseCase userBiometriaUseCase;
    private final EnrollService enrollService;

    @GetMapping
    public ResponseEntity<UserBiometriaListResponse> list(
            @RequestParam(name = "tenant_user_id", required = false) Long tenantUserId,
            @RequestParam(name = "device_id", required = false) Long deviceId,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var result = userBiometriaUseCase.findAll(tenantUserId, deviceId, activo, page, size);
        return ResponseEntity.ok(UserBiometriaListResponse.from(result));
    }

    @PostMapping("/enroll/iniciar")
    public ResponseEntity<EnrollSessionResponse> initEnroll(@Valid @RequestBody InitEnrollRequest request) {
        var result = enrollService.initEnroll(request.tenantUserId(), request.deviceId(), request.fingerIndex());
        return ResponseEntity.status(HttpStatus.CREATED).body(EnrollSessionResponse.from(result));
    }

    @GetMapping("/enroll/pendiente")
    public ResponseEntity<PendingEnrollSessionResponse> pendingEnroll(@RequestParam("device_code") String deviceCode) {
        return userBiometriaUseCase.findPendingEnrollSession(deviceCode)
                .map(PendingEnrollSessionResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/enroll/completar")
    public ResponseEntity<UserBiometriaResponse> completeEnroll(@Valid @RequestBody CompleteEnrollHttpRequest request) {
        var result = enrollService.completeEnroll(
                request.enrollToken(),
                request.deviceCode(),
                request.fingerIndex(),
                request.encryptedTemplateBase64(),
                request.encryptedAesKeyBase64(),
                request.ivBase64(),
                request.tagBase64(),
                request.capturedAtUtc()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(UserBiometriaResponse.from(result));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UserBiometriaResponse> patchActivo(
            @PathVariable Long id,
            @Valid @RequestBody PatchBiometriaRequest request
    ) {
        var result = userBiometriaUseCase.patchActivo(id, request.activo());
        return ResponseEntity.ok(UserBiometriaResponse.from(result));
    }
}
