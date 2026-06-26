package trazzo.back.reports.application.usecase;

import trazzo.back.reports.application.dto.command.GetMonthlyClosureCommand;
import trazzo.back.reports.application.dto.result.MonthlyClosureResult;
import trazzo.back.reports.application.ports.in.GetMonthlyClosureUseCase;
import trazzo.back.reports.application.ports.out.MonthlyClosureRepositoryPort;
import trazzo.back.reports.domain.exception.MonthlyClosureNotFoundException;
import trazzo.back.reports.domain.model.closure.MonthlyClosure;

public class GetMonthlyClosureService implements GetMonthlyClosureUseCase {

    private final MonthlyClosureRepositoryPort closureRepository;

    public GetMonthlyClosureService(MonthlyClosureRepositoryPort closureRepository) {
        this.closureRepository = closureRepository;
    }

    @Override
    public MonthlyClosureResult execute(GetMonthlyClosureCommand command) {
        MonthlyClosure closure = closureRepository.findById(command.id())
                .orElseThrow(() -> new MonthlyClosureNotFoundException(command.id()));
        return new MonthlyClosureResult(
                closure.getId(), closure.getMonth(), closure.getYear(),
                closure.getTotalEmployees(), closure.getExcelReportUrl(),
                closure.getPdfReportUrl(), closure.getCreatedAt());
    }
}
