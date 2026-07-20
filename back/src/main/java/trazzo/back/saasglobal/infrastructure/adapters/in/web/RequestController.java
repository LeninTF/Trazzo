package trazzo.back.saasglobal.infrastructure.adapters.in.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import trazzo.back.saasglobal.application.dto.command.SubmitRequestCommand;
import trazzo.back.saasglobal.application.dto.result.RequestResult;
import trazzo.back.saasglobal.application.port.in.RequestUseCase;
import trazzo.back.saasglobal.infrastructure.adapters.in.web.dto.SubmitRequestRequest;

/**
 * Public, unauthenticated endpoint for the marketing site's contact form (see SecurityConfig
 * permitAll). Admin-side request management lives in SaasRequestController under /saas/requests.
 */
@RestController
@RequestMapping("/requests")
@RequiredArgsConstructor
public class RequestController {

    private final RequestUseCase requestUseCase;

    @PostMapping
    public ResponseEntity<RequestResult> submit(@RequestBody @Valid SubmitRequestRequest request) {
        RequestResult result = requestUseCase.submit(new SubmitRequestCommand(
                request.type(), request.name(), request.lastName(), request.email(),
                request.phoneNumber(), request.taxId(), request.companyName(), request.message()));
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
