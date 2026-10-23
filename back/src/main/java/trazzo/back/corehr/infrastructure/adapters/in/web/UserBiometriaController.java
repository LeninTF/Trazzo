package trazzo.back.corehr.infrastructure.adapters.in.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import trazzo.back.corehr.application.port.in.UserBiometriaUseCase;
import trazzo.back.corehr.infrastructure.adapters.in.web.dto.*;
import trazzo.back.corehr.infrastructure.adapters.out.enroll.EnrollService;
import trazzo.back.shared.security.DeviceAgentPrincipal;

@RestController
@RequestMapping("/corehr/biometria")
@RequiredArgsConstructor
public class UserBiometriaController {

    private final UserBiometriaUseCase userBiometriaUseCase;
    private final EnrollService enrollService;

    @GetMapping
    @PreAuthorize("@authz.any('configuracion-tenant.gestionar-metodos', 'control-asistencia.operador-asistencia', 'gestion-trabajadores.editar')")
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
    @PreAuthorize("@authz.any('configuracion-tenant.gestionar-metodos', 'control-asistencia.operador-asistencia', 'gestion-trabajadores.editar')")
    public ResponseEntity<EnrollSessionResponse> initEnroll(@Valid @RequestBody InitEnrollRequest request) {
        var result = enrollService.initEnroll(request.tenantUserId(), request.deviceId(), request.fingerIndex());
        return ResponseEntity.status(HttpStatus.CREATED).body(EnrollSessionResponse.from(result));
    }

    @GetMapping("/enroll/pendiente")
    @PreAuthorize(DeviceAgentPrincipal.AGENT_OR_ATTENDANCE_OPERATOR)
    public ResponseEntity<PendingEnrollSessionResponse> pendingEnroll(@RequestParam("device_code") String deviceCode) {
        DeviceAgentPrincipal.requireOwnDevice(deviceCode);
        return userBiometriaUseCase.findPendingEnrollSession(deviceCode)
                .map(PendingEnrollSessionResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/enroll/completar")
    @PreAuthorize(DeviceAgentPrincipal.AGENT_OR_ATTENDANCE_OPERATOR)
    public ResponseEntity<UserBiometriaResponse> completeEnroll(@Valid @RequestBody CompleteEnrollHttpRequest request) {
        DeviceAgentPrincipal.requireOwnDevice(request.deviceCode());
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
    @PreAuthorize("@authz.any('configuracion-tenant.gestionar-metodos', 'control-asistencia.operador-asistencia', 'gestion-trabajadores.editar')")
    public ResponseEntity<UserBiometriaResponse> patchActivo(
            @PathVariable Long id,
            @Valid @RequestBody PatchBiometriaRequest request
    ) {
        var result = userBiometriaUseCase.patchActivo(id, request.activo());
        return ResponseEntity.ok(UserBiometriaResponse.from(result));
    }
}
