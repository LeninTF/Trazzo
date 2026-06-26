package trazzo.back.reports.application.dto.result;

import java.time.LocalDateTime;
import java.util.UUID;

public record MonthlyClosureResult(
    UUID id,
    int month,
    int year,
    int totalEmployees,
    String excelReportUrl,
    String pdfReportUrl,
    LocalDateTime createdAt
) {}