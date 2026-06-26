package trazzo.back.reports.application.ports.in;

import java.util.List;
import trazzo.back.reports.application.dto.command.ListMonthlyClosuresCommand;
import trazzo.back.reports.application.dto.result.MonthlyClosureResult;

public interface ListMonthlyClosureUseCase {
    List<MonthlyClosureResult> execute(ListMonthlyClosuresCommand command);
}
