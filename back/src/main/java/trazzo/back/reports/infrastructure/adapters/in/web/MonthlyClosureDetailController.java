package trazzo.back.reports.infrastructure.adapters.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import trazzo.back.reports.application.dto.result.MonthlyClosureDetailResult;
import trazzo.back.reports.application.ports.in.GetMonthlyClosureDetailUseCase;
import trazzo.back.reports.infrastructure.adapters.in.web.dto.MonthlyClosureDetailResponse;

import java.util.UUID;

@RestController
@RequestMapping("/reports/monthly-closure-details")
public class MonthlyClosureDetailController {

    private final GetMonthlyClosureDetailUseCase detailUseCase;

    public MonthlyClosureDetailController(GetMonthlyClosureDetailUseCase detailUseCase) {
        this.detailUseCase = detailUseCase;
    }

    @GetMapping("/{id}")
    public ResponseEntity<MonthlyClosureDetailResponse> getById(@PathVariable UUID id) {
        MonthlyClosureDetailResult result = detailUseCase.execute(id);
        return ResponseEntity.ok(toResponse(result));
    }

    private MonthlyClosureDetailResponse toResponse(MonthlyClosureDetailResult result) {
        return new MonthlyClosureDetailResponse(
                result.id(), result.monthClosureId(), result.tenantUserId(),
                result.tenantUserFullName(), result.tenantUserDocument(),
                result.departmentName(), result.roleName(),
                result.totalWorkedHours(), result.totalTardinessMinutes(),
                result.totalAbsences(), result.totalOvertimeHours(),
                result.createdAt());
    }
}
