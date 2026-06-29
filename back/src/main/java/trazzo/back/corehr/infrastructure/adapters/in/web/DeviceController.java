package trazzo.back.corehr.infrastructure.adapters.in.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import trazzo.back.corehr.application.dto.command.CreateDeviceCommand;
import trazzo.back.corehr.application.dto.command.PatchDeviceCommand;
import trazzo.back.corehr.application.port.in.DeviceUseCase;
import trazzo.back.corehr.infrastructure.adapters.in.web.dto.CreateDeviceRequest;
import trazzo.back.corehr.infrastructure.adapters.in.web.dto.DeviceListResponse;
import trazzo.back.corehr.infrastructure.adapters.in.web.dto.DeviceResponse;
import trazzo.back.corehr.infrastructure.adapters.in.web.dto.PatchDeviceRequest;

@RestController
@RequestMapping("/corehr/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceUseCase deviceUseCase;

    @GetMapping
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
    public ResponseEntity<DeviceResponse> create(@Valid @RequestBody CreateDeviceRequest request) {
        var command = new CreateDeviceCommand(request.code(), request.name(), request.branchId(),
                request.ip(), request.puerto(), request.ubicacion());
        var result = deviceUseCase.create(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(DeviceResponse.from(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeviceResponse> getById(@PathVariable Long id) {
        return deviceUseCase.findById(id)
                .map(result -> ResponseEntity.ok(DeviceResponse.from(result)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}")
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
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        deviceUseCase.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
