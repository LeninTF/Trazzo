package trazzo.back.saasglobal.infrastructure.adapters.in.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import trazzo.back.saasglobal.application.dto.command.AssignSaasUserRolesCommand;
import trazzo.back.saasglobal.application.dto.command.CreateSaasUserCommand;
import trazzo.back.saasglobal.application.dto.command.UpdateSaasUserCommand;
import trazzo.back.saasglobal.application.port.in.SaasUserUseCase;
import trazzo.back.saasglobal.infrastructure.adapters.in.web.dto.AssignSaasUserRolesRequest;
import trazzo.back.saasglobal.infrastructure.adapters.in.web.dto.CreateSaasUserRequest;
import trazzo.back.saasglobal.infrastructure.adapters.in.web.dto.SaasUserListResponse;
import trazzo.back.saasglobal.infrastructure.adapters.in.web.dto.SaasUserProfileResponse;
import trazzo.back.saasglobal.infrastructure.adapters.in.web.dto.UpdateSaasUserRequest;
import trazzo.back.shared.security.AuthenticatedUser;

@RestController
@RequestMapping("/saas/users")
@RequiredArgsConstructor
public class SaasUserController {

    private final SaasUserUseCase userUseCase;

    @GetMapping
    public ResponseEntity<SaasUserListResponse> listAll(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(SaasUserListResponse.from(userUseCase.listAll(search, page, size)));
    }

    @GetMapping("/me")
    public ResponseEntity<SaasUserProfileResponse> me(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(SaasUserProfileResponse.from(userUseCase.getById(principal.id().toString())));
    }

    @PatchMapping("/me")
    public ResponseEntity<SaasUserProfileResponse> updateMe(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestBody @Valid UpdateSaasUserRequest request) {
        var result = userUseCase.update(
                new UpdateSaasUserCommand(principal.id().toString(), request.email(), request.phone()));
        return ResponseEntity.ok(SaasUserProfileResponse.from(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SaasUserProfileResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(SaasUserProfileResponse.from(userUseCase.getById(id)));
    }

    @PostMapping
    public ResponseEntity<SaasUserProfileResponse> create(@RequestBody @Valid CreateSaasUserRequest request) {
        var result = userUseCase.create(new CreateSaasUserCommand(
                request.documentType(), request.documentValue(), request.name(),
                request.fatherSurname(), request.motherSurname(), request.email(),
                request.phone(), request.password(), request.roleIds()));
        return ResponseEntity.status(HttpStatus.CREATED).body(SaasUserProfileResponse.from(result));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<SaasUserProfileResponse> update(
            @PathVariable String id,
            @RequestBody @Valid UpdateSaasUserRequest request) {
        var result = userUseCase.update(new UpdateSaasUserCommand(id, request.email(), request.phone()));
        return ResponseEntity.ok(SaasUserProfileResponse.from(result));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        userUseCase.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/roles")
    public ResponseEntity<SaasUserProfileResponse> assignRoles(
            @PathVariable String id,
            @RequestBody @Valid AssignSaasUserRolesRequest request) {
        var result = userUseCase.assignRoles(new AssignSaasUserRolesCommand(id, request.roleIds()));
        return ResponseEntity.ok(SaasUserProfileResponse.from(result));
    }
}
