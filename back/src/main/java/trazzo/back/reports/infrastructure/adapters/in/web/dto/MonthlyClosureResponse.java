package trazzo.back.reports.infrastructure.adapters.in.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record MonthlyClosureResponse(
        UUID id, int month, int year, int totalEmployees,
        String excelReportUrl, String pdfReportUrl, LocalDateTime createdAt) {
}
