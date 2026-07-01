package trazzo.back.reports.application.usecase;

import trazzo.back.reports.application.dto.result.MonthlyClosureDetailResult;
import trazzo.back.reports.application.dto.result.MonthlyClosureWithDetailsResult;
import trazzo.back.reports.application.ports.in.GetMonthlyReportUseCase;
import trazzo.back.reports.application.ports.out.MonthlyClosureDetailRepositoryPort;
import trazzo.back.reports.application.ports.out.MonthlyClosureRepositoryPort;
import trazzo.back.reports.domain.exception.MonthlyClosureNotFoundException;
import trazzo.back.reports.domain.model.closure.MonthlyClosure;
import trazzo.back.reports.domain.model.closure.MonthlyClosureDetail;

import java.util.List;
import java.util.UUID;

public class GetMonthlyReportService implements GetMonthlyReportUseCase {

    private final MonthlyClosureRepositoryPort closureRepository;
    private final MonthlyClosureDetailRepositoryPort detailRepository;

    public GetMonthlyReportService(MonthlyClosureRepositoryPort closureRepository,
                                   MonthlyClosureDetailRepositoryPort detailRepository) {
        this.closureRepository = closureRepository;
        this.detailRepository = detailRepository;
    }

    @Override
    public MonthlyClosureWithDetailsResult execute(UUID id) {
        MonthlyClosure closure = closureRepository.findById(id)
                .orElseThrow(() -> new MonthlyClosureNotFoundException(id));

        List<MonthlyClosureDetail> details = detailRepository.findByMonthlyClosureId(id);

        List<MonthlyClosureDetailResult> detailResults = details.stream()
                .map(d -> new MonthlyClosureDetailResult(
                        d.getId(), d.getMonthClosureId(), d.getTenantUserId(),
                        d.getTenantUserFullName(), d.getTenantUserDocument(),
                        d.getDepartmentName(), d.getRoleName(),
                        d.getTotalWorkedHours(), d.getTotalTardinessMinutes(),
                        d.getTotalAbsences(), d.getTotalOvertimeHours(),
                        d.getCreatedAt()))
                .toList();

        return new MonthlyClosureWithDetailsResult(
                closure.getId(), closure.getMonth(), closure.getYear(),
                closure.getTotalEmployees(), closure.getExcelReportUrl(),
                closure.getPdfReportUrl(), closure.getCreatedAt(), detailResults);
    }
}
