package trazzo.back.reports.application.dto.result;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record MonthlyClosureWithDetailsResult(
        UUID id,
        int month,
        int year,
        int totalEmployees,
        String excelReportUrl,
        String pdfReportUrl,
        LocalDateTime createdAt,
        List<MonthlyClosureDetailResult> details) {

}
