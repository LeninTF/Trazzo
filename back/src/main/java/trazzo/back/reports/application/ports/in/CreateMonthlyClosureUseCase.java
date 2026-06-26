package trazzo.back.reports.application.ports.in;

import trazzo.back.reports.application.dto.command.CreateMonthlyClosureCommand;
import trazzo.back.reports.application.dto.result.MonthlyClosureResult;

public interface CreateMonthlyClosureUseCase {
    MonthlyClosureResult execute(CreateMonthlyClosureCommand command);
}
