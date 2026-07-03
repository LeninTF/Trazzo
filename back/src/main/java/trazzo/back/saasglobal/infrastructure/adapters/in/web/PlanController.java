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
import trazzo.back.saasglobal.application.dto.command.CreatePlanCommand;
import trazzo.back.saasglobal.application.dto.command.UpdatePlanCommand;
import trazzo.back.saasglobal.application.dto.result.PlanResult;
import trazzo.back.saasglobal.application.port.in.PlanUseCase;
import trazzo.back.saasglobal.infrastructure.adapters.in.web.dto.CreatePlanRequest;
import trazzo.back.saasglobal.infrastructure.adapters.in.web.dto.UpdatePlanRequest;

@RestController
@RequestMapping("/saas/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanUseCase planUseCase;

    @GetMapping
    public ResponseEntity<List<PlanResult>> listAll() {
        return ResponseEntity.ok(planUseCase.listAll());
    }

    @GetMapping("/active")
    public ResponseEntity<List<PlanResult>> listActive() {
        return ResponseEntity.ok(planUseCase.listActive());
    }

    @PostMapping
    public ResponseEntity<PlanResult> create(@RequestBody @Valid CreatePlanRequest request) {
        PlanResult result = planUseCase.create(
                new CreatePlanCommand(request.name(), request.price(), request.currency(), request.billingPeriod()));
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlanResult> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(planUseCase.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlanResult> update(
            @PathVariable Integer id,
            @RequestBody @Valid UpdatePlanRequest request) {
        return ResponseEntity.ok(planUseCase.update(
                new UpdatePlanCommand(id, request.name(), request.price(), request.currency(), request.billingPeriod())));
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<PlanResult> activate(@PathVariable Integer id) {
        return ResponseEntity.ok(planUseCase.activate(id));
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<PlanResult> deactivate(@PathVariable Integer id) {
        return ResponseEntity.ok(planUseCase.deactivate(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        planUseCase.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
