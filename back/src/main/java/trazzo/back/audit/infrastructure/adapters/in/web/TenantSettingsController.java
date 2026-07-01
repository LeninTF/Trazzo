package trazzo.back.audit.infrastructure.adapters.in.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import trazzo.back.audit.application.port.in.TenantSettingsUseCase;
import trazzo.back.audit.infrastructure.adapters.in.web.dto.TenantSettingsRecordListResponse;
import trazzo.back.audit.infrastructure.adapters.in.web.dto.TenantSettingsRecordResponse;

@RestController
@RequestMapping("/audit/tenant-settings")
@RequiredArgsConstructor
public class TenantSettingsController {

    private final TenantSettingsUseCase tenantSettingsUseCase;

    @GetMapping
    public ResponseEntity<TenantSettingsRecordListResponse> findAll(
            @RequestParam(name = "tenant_setting_id", required = false) String tenantSettingId,
            @RequestParam(name = "user_id", required = false) String userId,
            @RequestParam(name = "change_reason", required = false) String changeReason,
            @RequestParam(name = "fecha_desde", required = false) String fechaDesde,
            @RequestParam(name = "fecha_hasta", required = false) String fechaHasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort) {
        var result = tenantSettingsUseCase.findAll(tenantSettingId, userId, changeReason, fechaDesde, fechaHasta, page, size, sort);
        return ResponseEntity.ok(TenantSettingsRecordListResponse.from(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TenantSettingsRecordResponse> findById(@PathVariable Long id) {
        var result = tenantSettingsUseCase.findById(id);
        return ResponseEntity.ok(TenantSettingsRecordResponse.from(result));
    }
}
