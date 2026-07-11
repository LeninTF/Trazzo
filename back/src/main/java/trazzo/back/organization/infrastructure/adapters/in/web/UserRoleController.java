package trazzo.back.organization.infrastructure.adapters.in.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import trazzo.back.organization.application.dto.command.AssignRoleToUserCommand;
import trazzo.back.organization.application.dto.result.UserRoleAssignmentResult;
import trazzo.back.organization.application.port.in.UserRoleUseCase;
import trazzo.back.organization.infrastructure.adapters.in.web.dto.AssignRoleRequest;

import java.util.List;

@RestController
@RequestMapping("/org/users/{tenantUserId}/roles")
@RequiredArgsConstructor
public class UserRoleController {

    private final UserRoleUseCase userRoleUseCase;

    @GetMapping
    public ResponseEntity<List<UserRoleAssignmentResult>> list(@PathVariable Long tenantUserId) {
        return ResponseEntity.ok(userRoleUseCase.findByTenantUserId(tenantUserId));
    }

    @PostMapping
    public ResponseEntity<UserRoleAssignmentResult> assign(
            @PathVariable Long tenantUserId,
            @Valid @RequestBody AssignRoleRequest request
    ) {
        var result = userRoleUseCase.assign(tenantUserId,
                new AssignRoleToUserCommand(request.roleId(), request.departmentId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @DeleteMapping("/{assignmentId}")
    public ResponseEntity<Void> remove(
            @PathVariable Long tenantUserId,
            @PathVariable Long assignmentId
    ) {
        userRoleUseCase.remove(tenantUserId, assignmentId);
        return ResponseEntity.noContent().build();
    }
}
