package trazzo.back.reports.application.ports.in;

import trazzo.back.reports.application.dto.command.GetMonthlyClosureCommand;
import trazzo.back.reports.application.dto.result.MonthlyClosureResult;

public interface GetMonthlyClosureUseCase {
    MonthlyClosureResult execute(GetMonthlyClosureCommand command);
}
