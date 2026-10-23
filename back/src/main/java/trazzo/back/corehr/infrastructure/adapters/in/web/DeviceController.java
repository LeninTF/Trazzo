package trazzo.back.corehr.infrastructure.adapters.in.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import trazzo.back.corehr.application.dto.command.CreateDeviceCommand;
import trazzo.back.corehr.application.dto.command.PatchDeviceCommand;
import trazzo.back.corehr.application.port.in.DeviceUseCase;
import trazzo.back.corehr.application.usecase.DeviceAgentTokenService;
import trazzo.back.corehr.infrastructure.adapters.in.web.dto.AgentTokenResponse;
import trazzo.back.corehr.infrastructure.adapters.in.web.dto.CreateDeviceRequest;
import trazzo.back.corehr.infrastructure.adapters.in.web.dto.DeviceListResponse;
import trazzo.back.corehr.infrastructure.adapters.in.web.dto.DeviceResponse;
import trazzo.back.corehr.infrastructure.adapters.in.web.dto.PatchDeviceRequest;
import trazzo.back.shared.tenancy.TenantContext;

@RestController
@RequestMapping("/corehr/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceUseCase deviceUseCase;
    private final DeviceAgentTokenService deviceAgentTokenService;

    @GetMapping
    @PreAuthorize("@authz.any('configuracion-tenant.gestionar-metodos', 'control-asistencia.operador-asistencia')")
    public ResponseEntity<DeviceListResponse> list(
            @RequestParam(name = "branch_id", required = false) Long branchId,
            @RequestParam(required = false) Boolean state,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var result = deviceUseCase.findAll(branchId, state, page, size);
        return ResponseEntity.ok(DeviceListResponse.from(result));
    }

    @PostMapping
    @PreAuthorize("@authz.any('configuracion-tenant.gestionar-metodos')")
    public ResponseEntity<DeviceResponse> create(@Valid @RequestBody CreateDeviceRequest request) {
        var command = new CreateDeviceCommand(request.code(), request.name(), request.branchId(),
                request.ip(), request.puerto(), request.ubicacion());
        var result = deviceUseCase.create(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(DeviceResponse.from(result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authz.any('configuracion-tenant.gestionar-metodos', 'control-asistencia.operador-asistencia')")
    public ResponseEntity<DeviceResponse> getById(@PathVariable Long id) {
        return deviceUseCase.findById(id)
                .map(result -> ResponseEntity.ok(DeviceResponse.from(result)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@authz.any('configuracion-tenant.gestionar-metodos')")
    public ResponseEntity<DeviceResponse> patch(
            @PathVariable Long id,
            @Valid @RequestBody PatchDeviceRequest request
    ) {
        var command = new PatchDeviceCommand(request.name(), request.branchId(), request.ip(),
                request.puerto(), request.ubicacion(), request.state());
        var result = deviceUseCase.patch(id, command);
        return ResponseEntity.ok(DeviceResponse.from(result));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authz.any('configuracion-tenant.gestionar-metodos')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        // El token de un lector borrado no debe seguir abriendo los endpoints del agente.
        deviceAgentTokenService.revoke(TenantContext.get(), id);
        deviceUseCase.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // El token solo viaja en esta respuesta: emitir otro revoca el anterior.
    @PostMapping("/{id}/agent-token")
    @PreAuthorize("@authz.any('configuracion-tenant.gestionar-metodos')")
    public ResponseEntity<AgentTokenResponse> issueAgentToken(@PathVariable Long id) {
        var issued = deviceAgentTokenService.issue(TenantContext.get(), id);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(AgentTokenResponse.from(issued));
    }

    @DeleteMapping("/{id}/agent-token")
    @PreAuthorize("@authz.any('configuracion-tenant.gestionar-metodos')")
    public ResponseEntity<Void> revokeAgentToken(@PathVariable Long id) {
        deviceAgentTokenService.revoke(TenantContext.get(), id);
        return ResponseEntity.noContent().build();
    }
}
