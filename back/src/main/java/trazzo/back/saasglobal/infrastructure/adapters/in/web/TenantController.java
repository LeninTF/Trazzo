package trazzo.back.saasglobal.infrastructure.adapters.in.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import trazzo.back.saasglobal.application.dto.command.CreateTrialTenantCommand;
import trazzo.back.saasglobal.application.dto.result.TenantResultDto;
import trazzo.back.saasglobal.application.port.in.CreateTrialTenantUseCase;
import trazzo.back.saasglobal.infrastructure.adapters.in.web.dto.CreateTrialTenantRequest;

@RestController
@RequestMapping("/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final CreateTrialTenantUseCase createTrialTenantUseCase;

    @PostMapping("/trial")
    public ResponseEntity<TenantResultDto> createTrial(@RequestBody @Valid CreateTrialTenantRequest request) {
        CreateTrialTenantCommand command = new CreateTrialTenantCommand(
                request.subDomain(),
                request.planId(),
                request.holdingId(),
                request.dbHost(),
                request.dbPort(),
                request.dbName(),
                request.dbUser(),
                request.dbPassword(),
                request.logoUrl(),
                request.slogan(),
                request.primaryColor(),
                request.secondaryColor()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(createTrialTenantUseCase.createTrial(command));
    }
}
