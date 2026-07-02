package trazzo.back.reports.infrastructure.adapters.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import trazzo.back.reports.application.dto.result.MonthlyClosureDetailResult;
import trazzo.back.reports.application.dto.result.MonthlyClosureWithDetailsResult;
import trazzo.back.reports.application.ports.in.GetMonthlyReportUseCase;
import trazzo.back.reports.infrastructure.adapters.in.web.dto.MonthlyClosureDetailResponse;
import trazzo.back.reports.infrastructure.adapters.in.web.dto.MonthlyClosureWithDetailsResponse;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/reports/monthly-reports")
public class MonthlyReportController {

    private final GetMonthlyReportUseCase reportUseCase;

    public MonthlyReportController(GetMonthlyReportUseCase reportUseCase) {
        this.reportUseCase = reportUseCase;
    }

    @GetMapping("/{id}")
    public ResponseEntity<MonthlyClosureWithDetailsResponse> getById(@PathVariable UUID id) {
        MonthlyClosureWithDetailsResult result = reportUseCase.execute(id);
        return ResponseEntity.ok(toResponse(result));
    }

    private MonthlyClosureWithDetailsResponse toResponse(MonthlyClosureWithDetailsResult result) {
        List<MonthlyClosureDetailResponse> details = result.details().stream()
                .map(this::toDetailResponse)
                .toList();
        return new MonthlyClosureWithDetailsResponse(
                result.id(), result.month(), result.year(),
                result.totalEmployees(), result.excelReportUrl(),
                result.pdfReportUrl(), result.createdAt(), details);
    }

    private MonthlyClosureDetailResponse toDetailResponse(MonthlyClosureDetailResult detail) {
        return new MonthlyClosureDetailResponse(
                detail.id(), detail.monthClosureId(), detail.tenantUserId(),
                detail.tenantUserFullName(), detail.tenantUserDocument(),
                detail.departmentName(), detail.roleName(),
                detail.totalWorkedHours(), detail.totalTardinessMinutes(),
                detail.totalAbsences(), detail.totalOvertimeHours(),
                detail.createdAt());
    }
}
