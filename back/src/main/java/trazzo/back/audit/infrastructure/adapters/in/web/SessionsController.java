package trazzo.back.audit.infrastructure.adapters.in.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import trazzo.back.audit.application.port.in.SessionUseCase;
import trazzo.back.audit.domain.model.tenant.SessionState;
import trazzo.back.audit.infrastructure.adapters.in.web.dto.SessionListResponse;
import trazzo.back.audit.infrastructure.adapters.in.web.dto.SessionResponse;

@RestController
@RequestMapping("/audit/sessions")
@RequiredArgsConstructor
public class SessionsController {

    private final SessionUseCase sessionUseCase;

    @GetMapping
    public ResponseEntity<SessionListResponse> findAll(
            @RequestParam(name = "tenant_user_id", required = false) String tenantUserId,
            @RequestParam(required = false) SessionState state,
            @RequestParam(name = "ip_address", required = false) String ipAddress,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort) {
        var result = sessionUseCase.findAll(tenantUserId, state, ipAddress, page, size, sort);
        return ResponseEntity.ok(SessionListResponse.from(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SessionResponse> findById(@PathVariable Long id) {
        var result = sessionUseCase.findById(id);
        return ResponseEntity.ok(SessionResponse.from(result));
    }
}
