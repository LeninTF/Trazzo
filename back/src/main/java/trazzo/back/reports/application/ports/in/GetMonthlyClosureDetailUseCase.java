package trazzo.back.reports.application.ports.in;

import java.util.UUID;
import trazzo.back.reports.application.dto.result.MonthlyClosureDetailResult;

public interface GetMonthlyClosureDetailUseCase {
     MonthlyClosureDetailResult execute(UUID id);
}
