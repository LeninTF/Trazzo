package trazzo.back.saasglobal.infrastructure.adapters.in.web;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import trazzo.back.saasglobal.application.dto.result.PlanResult;
import trazzo.back.saasglobal.application.port.in.PlanUseCase;

/**
 * Public, unauthenticated endpoint for the marketing site's pricing section (see SecurityConfig
 * permitAll). Admin-side plan management lives in PlanController under /saas/plans.
 */
@RestController
@RequestMapping("/public/plans")
@RequiredArgsConstructor
public class PublicPlanController {

    private final PlanUseCase planUseCase;

    @GetMapping
    public ResponseEntity<List<PlanResult>> listActive() {
        return ResponseEntity.ok(planUseCase.listActive());
    }
}
