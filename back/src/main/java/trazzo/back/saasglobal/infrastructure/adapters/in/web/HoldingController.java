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
import trazzo.back.saasglobal.application.dto.command.CreateHoldingCommand;
import trazzo.back.saasglobal.application.dto.command.UpdateHoldingCommand;
import trazzo.back.saasglobal.application.dto.result.HoldingResult;
import trazzo.back.saasglobal.application.port.in.HoldingUseCase;
import trazzo.back.saasglobal.infrastructure.adapters.in.web.dto.CreateHoldingRequest;
import trazzo.back.saasglobal.infrastructure.adapters.in.web.dto.UpdateHoldingRequest;

@RestController
@RequestMapping("/saas/holdings")
@RequiredArgsConstructor
public class HoldingController {

    private final HoldingUseCase holdingUseCase;

    @GetMapping
    public ResponseEntity<List<HoldingResult>> listAll() {
        return ResponseEntity.ok(holdingUseCase.listAll());
    }

    @PostMapping
    public ResponseEntity<HoldingResult> create(@RequestBody @Valid CreateHoldingRequest request) {
        HoldingResult result = holdingUseCase.create(
                new CreateHoldingCommand(request.taxId(), request.legalName(), request.type()));
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<HoldingResult> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(holdingUseCase.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<HoldingResult> update(
            @PathVariable Integer id,
            @RequestBody @Valid UpdateHoldingRequest request) {
        return ResponseEntity.ok(holdingUseCase.update(
                new UpdateHoldingCommand(id, request.legalName(), request.type())));
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<HoldingResult> activate(@PathVariable Integer id) {
        return ResponseEntity.ok(holdingUseCase.activate(id));
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<HoldingResult> deactivate(@PathVariable Integer id) {
        return ResponseEntity.ok(holdingUseCase.deactivate(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        holdingUseCase.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
