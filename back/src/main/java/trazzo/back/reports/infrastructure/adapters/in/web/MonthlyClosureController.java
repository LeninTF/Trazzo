package trazzo.back.reports.infrastructure.adapters.in.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import trazzo.back.reports.application.dto.command.CreateMonthlyClosureCommand;
import trazzo.back.reports.application.dto.command.GetMonthlyClosureCommand;
import trazzo.back.reports.application.dto.command.ListMonthlyClosuresCommand;
import trazzo.back.reports.application.dto.result.MonthlyClosureResult;
import trazzo.back.reports.application.ports.in.CreateMonthlyClosureUseCase;
import trazzo.back.reports.application.ports.in.GetMonthlyClosureUseCase;
import trazzo.back.reports.application.ports.in.ListMonthlyClosureUseCase;
import trazzo.back.reports.infrastructure.adapters.in.web.dto.CreateMonthlyClosureRequest;
import trazzo.back.reports.infrastructure.adapters.in.web.dto.MonthlyClosureResponse;
import trazzo.back.shared.security.AuthenticatedUser;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/reports/monthly-closures")
public class MonthlyClosureController {

    private final CreateMonthlyClosureUseCase createUseCase;
    private final GetMonthlyClosureUseCase getUseCase;
    private final ListMonthlyClosureUseCase listUseCase;

    public MonthlyClosureController(CreateMonthlyClosureUseCase createUseCase,
                                     GetMonthlyClosureUseCase getUseCase,
                                     ListMonthlyClosureUseCase listUseCase) {
        this.createUseCase = createUseCase;
        this.getUseCase = getUseCase;
        this.listUseCase = listUseCase;
    }

    @PostMapping
    public ResponseEntity<MonthlyClosureResponse> create(
            @Valid @RequestBody CreateMonthlyClosureRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        CreateMonthlyClosureCommand command = new CreateMonthlyClosureCommand(
                request.month(), request.year(), user.id());
        MonthlyClosureResult result = createUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MonthlyClosureResponse> getById(@PathVariable UUID id) {
        GetMonthlyClosureCommand command = new GetMonthlyClosureCommand(id);
        MonthlyClosureResult result = getUseCase.execute(command);
        return ResponseEntity.ok(toResponse(result));
    }

    @GetMapping
    public ResponseEntity<List<MonthlyClosureResponse>> list(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        ListMonthlyClosuresCommand command = new ListMonthlyClosuresCommand(year, month);
        List<MonthlyClosureResult> results = listUseCase.execute(command);
        List<MonthlyClosureResponse> responses = results.stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    private MonthlyClosureResponse toResponse(MonthlyClosureResult result) {
        return new MonthlyClosureResponse(
                result.id(), result.month(), result.year(),
                result.totalEmployees(), result.excelReportUrl(),
                result.pdfReportUrl(), result.createdAt());
    }
}
