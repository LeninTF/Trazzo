package trazzo.back.saasglobal.infrastructure.adapters.in.web;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import trazzo.back.saasglobal.application.dto.command.CreateRoleCommand;
import trazzo.back.saasglobal.application.dto.command.UpdateRoleCommand;
import trazzo.back.saasglobal.application.dto.command.UpdateRolePermissionsCommand;
import trazzo.back.saasglobal.application.dto.result.SaasRoleResult;
import trazzo.back.saasglobal.application.port.in.SaasRoleUseCase;
import trazzo.back.saasglobal.infrastructure.adapters.in.web.dto.CreateRoleRequest;
import trazzo.back.saasglobal.infrastructure.adapters.in.web.dto.UpdateRolePermissionsRequest;
import trazzo.back.saasglobal.infrastructure.adapters.in.web.dto.UpdateRoleRequest;

@RestController
@RequestMapping("/saas/roles")
@RequiredArgsConstructor
public class SaasRoleController {

    private final SaasRoleUseCase roleUseCase;

    @GetMapping
    public ResponseEntity<List<SaasRoleResult>> listAll() {
        return ResponseEntity.ok(roleUseCase.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SaasRoleResult> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(roleUseCase.getById(id));
    }

    @PostMapping
    public ResponseEntity<SaasRoleResult> create(@RequestBody @Valid CreateRoleRequest request) {
        SaasRoleResult result = roleUseCase.create(
                new CreateRoleCommand(request.name(), request.displayName(), request.description()));
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SaasRoleResult> update(
            @PathVariable Integer id,
            @RequestBody @Valid UpdateRoleRequest request) {
        return ResponseEntity.ok(roleUseCase.update(
                new UpdateRoleCommand(id, request.name(), request.displayName(), request.description())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        roleUseCase.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/permissions")
    public ResponseEntity<SaasRoleResult> updatePermissions(
            @PathVariable Integer id,
            @RequestBody @Valid UpdateRolePermissionsRequest request) {
        return ResponseEntity.ok(roleUseCase.updatePermissions(
                new UpdateRolePermissionsCommand(id, request.permissions())));
    }
}
