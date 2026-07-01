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
import trazzo.back.saasglobal.application.dto.command.CreateFeatureCommand;
import trazzo.back.saasglobal.application.dto.command.UpdateFeatureCommand;
import trazzo.back.saasglobal.application.dto.result.FeatureResult;
import trazzo.back.saasglobal.application.port.in.FeatureUseCase;
import trazzo.back.saasglobal.infrastructure.adapters.in.web.dto.CreateFeatureRequest;
import trazzo.back.saasglobal.infrastructure.adapters.in.web.dto.UpdateFeatureRequest;

@RestController
@RequestMapping("/api/v1/saas/features")
@RequiredArgsConstructor
public class FeatureController {

    private final FeatureUseCase featureUseCase;

    @GetMapping
    public ResponseEntity<List<FeatureResult>> listAll() {
        return ResponseEntity.ok(featureUseCase.listAll());
    }

    @PostMapping
    public ResponseEntity<FeatureResult> create(@RequestBody @Valid CreateFeatureRequest request) {
        FeatureResult result = featureUseCase.create(
                new CreateFeatureCommand(request.name(), request.description()));
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FeatureResult> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(featureUseCase.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FeatureResult> update(
            @PathVariable Integer id,
            @RequestBody @Valid UpdateFeatureRequest request) {
        return ResponseEntity.ok(featureUseCase.update(
                new UpdateFeatureCommand(id, request.name(), request.description())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Integer id) {
        featureUseCase.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
