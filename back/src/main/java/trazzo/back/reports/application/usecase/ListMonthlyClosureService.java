package trazzo.back.reports.application.usecase;

import trazzo.back.reports.application.dto.command.ListMonthlyClosuresCommand;
import trazzo.back.reports.application.dto.result.MonthlyClosureResult;
import trazzo.back.reports.application.ports.in.ListMonthlyClosureUseCase;
import trazzo.back.reports.application.ports.out.MonthlyClosureRepositoryPort;
import trazzo.back.reports.domain.model.closure.MonthlyClosure;

import java.util.List;

public class ListMonthlyClosureService implements ListMonthlyClosureUseCase {

    private final MonthlyClosureRepositoryPort closureRepository;

    public ListMonthlyClosureService(MonthlyClosureRepositoryPort closureRepository) {
        this.closureRepository = closureRepository;
    }

    @Override
    public List<MonthlyClosureResult> execute(ListMonthlyClosuresCommand command) {
        if (command.year() != null && command.month() != null) {
            List<MonthlyClosure> closures = closureRepository.findByMonthAndYear(command.month(), command.year());
            return closures.stream()
                    .map(c -> new MonthlyClosureResult(
                            c.getId(), c.getMonth(), c.getYear(),
                            c.getTotalEmployees(), c.getExcelReportUrl(),
                            c.getPdfReportUrl(), c.getCreatedAt()))
                    .toList();
        }
        List<MonthlyClosure> closures = closureRepository.findAll();
        return closures.stream()
                .map(c -> new MonthlyClosureResult(
                        c.getId(), c.getMonth(), c.getYear(),
                        c.getTotalEmployees(), c.getExcelReportUrl(),
                        c.getPdfReportUrl(), c.getCreatedAt()))
                .toList();
    }
}
