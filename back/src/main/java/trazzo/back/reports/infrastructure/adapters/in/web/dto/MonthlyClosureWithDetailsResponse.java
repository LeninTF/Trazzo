package trazzo.back.reports.infrastructure.adapters.in.web.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record MonthlyClosureWithDetailsResponse(
        UUID id, int month, int year, int totalEmployees,
        String excelReportUrl, String pdfReportUrl,
        LocalDateTime createdAt,
        List<MonthlyClosureDetailResponse> details) {
}
