package trazzo.back.saasglobal.infrastructure.adapters.in.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import trazzo.back.saasglobal.application.dto.result.PaginatedResult;
import trazzo.back.saasglobal.application.dto.result.TenantMetricsResult;
import trazzo.back.saasglobal.application.dto.result.TenantResult;
import trazzo.back.saasglobal.application.port.in.SaasTenantUseCase;
import trazzo.back.saasglobal.infrastructure.adapters.in.web.dto.UpdateTenantBrandingRequest;

// Admin-side read/lifecycle management of tenants. Creation stays at POST /tenants/trial
// (TenantController) — that flow already provisions the schema + activates + creates the
// trial subscription, no need to duplicate it here.
@RestController
@RequestMapping("/saas/tenants")
@RequiredArgsConstructor
public class SaasTenantController {

    private final SaasTenantUseCase tenantUseCase;

    @GetMapping
    public ResponseEntity<PaginatedResult<TenantResult>> listAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer planId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(tenantUseCase.listAll(search, planId, status, page, size));
    }

    @GetMapping("/metrics")
    public ResponseEntity<TenantMetricsResult> getMetrics() {
        return ResponseEntity.ok(tenantUseCase.getMetrics());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TenantResult> getById(@PathVariable String id) {
        return ResponseEntity.ok(tenantUseCase.getById(id));
    }

    @PutMapping("/{id}/suspend")
    public ResponseEntity<TenantResult> suspend(@PathVariable String id) {
        return ResponseEntity.ok(tenantUseCase.suspend(id));
    }

    @PutMapping("/{id}/reactivate")
    public ResponseEntity<TenantResult> reactivate(@PathVariable String id) {
        return ResponseEntity.ok(tenantUseCase.reactivate(id));
    }

    @PutMapping("/{id}/branding")
    public ResponseEntity<TenantResult> updateBranding(
            @PathVariable String id,
            @RequestBody UpdateTenantBrandingRequest request) {
        return ResponseEntity.ok(tenantUseCase.updateBranding(
                id, request.logoUrl(), request.slogan(), request.primaryColor(), request.secondaryColor()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        tenantUseCase.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
