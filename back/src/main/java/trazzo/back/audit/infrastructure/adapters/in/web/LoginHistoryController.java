package trazzo.back.audit.infrastructure.adapters.in.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import trazzo.back.audit.application.port.in.LoginHistoryUseCase;
import trazzo.back.audit.domain.model.master.StatusLogin;
import trazzo.back.audit.infrastructure.adapters.in.web.dto.LogInHistoryListResponse;
import trazzo.back.audit.infrastructure.adapters.in.web.dto.LogInHistoryResponse;

@RestController
@RequestMapping("/audit/login-history")
@RequiredArgsConstructor
public class LoginHistoryController {

    private final LoginHistoryUseCase loginHistoryUseCase;

    @GetMapping
    public ResponseEntity<LogInHistoryListResponse> findAll(
            @RequestParam(name = "user_id", required = false) String userId,
            @RequestParam(name = "attempted_email", required = false) String attemptedEmail,
            @RequestParam(required = false) StatusLogin status,
            @RequestParam(name = "fecha_desde", required = false) String fechaDesde,
            @RequestParam(name = "fecha_hasta", required = false) String fechaHasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort) {
        var result = loginHistoryUseCase.findAll(userId, attemptedEmail, status, fechaDesde, fechaHasta, page, size, sort);
        return ResponseEntity.ok(LogInHistoryListResponse.from(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LogInHistoryResponse> findById(@PathVariable String id) {
        var result = loginHistoryUseCase.findById(id);
        return ResponseEntity.ok(LogInHistoryResponse.from(result));
    }
}
