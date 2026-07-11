package trazzo.back.saasglobal.infrastructure.adapters.in.web;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import trazzo.back.saasglobal.application.dto.result.InvoiceResult;
import trazzo.back.saasglobal.application.dto.result.PaginatedResult;
import trazzo.back.saasglobal.application.dto.result.PlanResult;
import trazzo.back.saasglobal.application.port.in.InvoiceUseCase;
import trazzo.back.saasglobal.application.port.in.PlanUseCase;
import trazzo.back.saasglobal.application.port.out.TenantRepositoryPort;
import trazzo.back.saasglobal.application.port.out.UserRepositoryPort;
import trazzo.back.saasglobal.domain.model.iam.User;
import trazzo.back.saasglobal.domain.model.multitenancy.Tenant;
import trazzo.back.shared.security.AuthenticatedUser;

/**
 * Tenant-facing billing endpoints, deliberately outside "/saas/**" (which requires
 * ROLE_SAAS_ADMIN — a tenant user's JWT never carries it) so any authenticated tenant
 * user can see their own tenant's plan, browse plans, and view their own invoices.
 */
@RestController
@RequestMapping("/org/billing")
@RequiredArgsConstructor
public class TenantBillingController {

    private final PlanUseCase planUseCase;
    private final InvoiceUseCase invoiceUseCase;
    private final TenantRepositoryPort tenantRepository;
    private final UserRepositoryPort userRepository;

    @GetMapping("/plan")
    public ResponseEntity<PlanResult> getCurrentPlan(@AuthenticationPrincipal AuthenticatedUser principal) {
        Tenant tenant = tenantRepository.findById(resolveTenantId(principal))
                .orElseThrow(() -> new IllegalStateException("Tenant not found for authenticated user"));
        return ResponseEntity.ok(planUseCase.getById(tenant.getPlanId()));
    }

    @GetMapping("/plans")
    public ResponseEntity<List<PlanResult>> listAvailablePlans() {
        return ResponseEntity.ok(planUseCase.listActive());
    }

    @GetMapping("/invoices")
    public ResponseEntity<PaginatedResult<InvoiceResult>> listMyInvoices(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(invoiceUseCase.listAll(paymentStatus, resolveTenantId(principal), dateFrom, dateTo, page, size));
    }

    private String resolveTenantId(AuthenticatedUser principal) {
        User user = userRepository.findById(principal.id().toString())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + principal.id()));
        String tenantId = user.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Authenticated user has no tenant: " + principal.id());
        }
        return tenantId;
    }
}
