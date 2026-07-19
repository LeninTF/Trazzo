package trazzo.back.saasglobal.infrastructure.adapters.in.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import trazzo.back.saasglobal.application.dto.result.PaginatedResult;
import trazzo.back.saasglobal.application.dto.result.SubscriptionResult;
import trazzo.back.saasglobal.application.port.in.SubscriptionUseCase;

@RestController
@RequestMapping("/saas/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionUseCase subscriptionUseCase;

    @GetMapping
    public ResponseEntity<PaginatedResult<SubscriptionResult>> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(subscriptionUseCase.listAll(page, size));
    }
}
