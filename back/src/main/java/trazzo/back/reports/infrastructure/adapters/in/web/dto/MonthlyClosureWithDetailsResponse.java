package trazzo.back.reports.infrastructure.adapters.in.web.dto;

import java.util.List;
import java.util.UUID;

public record MonthlyClosureWithDetailsResponse(
        UUID id, int month, int year, int totalEmployees,
        String excelReportUrl, String pdfReportUrl,
        List<MonthlyClosureDetailResponse> details) {
}
