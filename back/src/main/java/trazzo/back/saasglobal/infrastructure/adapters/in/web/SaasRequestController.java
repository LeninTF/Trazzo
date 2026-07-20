package trazzo.back.saasglobal.infrastructure.adapters.in.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import trazzo.back.saasglobal.application.dto.command.AddCommentCommand;
import trazzo.back.saasglobal.application.dto.command.ChangeRequestStatusCommand;
import trazzo.back.saasglobal.application.dto.result.PaginatedResult;
import trazzo.back.saasglobal.application.dto.result.RequestCommentResult;
import trazzo.back.saasglobal.application.dto.result.RequestDetailResult;
import trazzo.back.saasglobal.application.dto.result.RequestResult;
import trazzo.back.saasglobal.application.port.in.RequestUseCase;
import trazzo.back.saasglobal.infrastructure.adapters.in.web.dto.AddRequestCommentRequest;
import trazzo.back.saasglobal.infrastructure.adapters.in.web.dto.ChangeRequestStatusRequest;
import trazzo.back.shared.security.AuthenticatedUser;

/** Admin-side management of contact-form requests; gated by /saas/** hasRole("admin_trazzo") in SecurityConfig. */
@RestController
@RequestMapping("/saas/requests")
@RequiredArgsConstructor
public class SaasRequestController {

    private final RequestUseCase requestUseCase;

    @GetMapping
    public ResponseEntity<PaginatedResult<RequestResult>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(requestUseCase.listAll(status, type, search, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RequestDetailResult> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(requestUseCase.getById(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<RequestResult> changeStatus(
            @PathVariable Integer id,
            @Valid @RequestBody ChangeRequestStatusRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        RequestResult result = requestUseCase.changeStatus(new ChangeRequestStatusCommand(
                id, request.status(), principal.id().toString(), request.comment()));
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<RequestCommentResult> addComment(
            @PathVariable Integer id,
            @Valid @RequestBody AddRequestCommentRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        RequestCommentResult result = requestUseCase.addComment(new AddCommentCommand(
                id, principal.id().toString(), request.comment()));
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
