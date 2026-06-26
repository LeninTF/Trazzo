package trazzo.back.reports.application.ports.in;

import java.util.UUID;
import trazzo.back.reports.application.dto.result.MonthlyClosureWithDetailsResult;

public interface GetMonthlyReportUseCase {
    MonthlyClosureWithDetailsResult execute(UUID id);

}
