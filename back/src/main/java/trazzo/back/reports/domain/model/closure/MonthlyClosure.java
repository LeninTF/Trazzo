package trazzo.back.reports.domain.model.closure;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

@Getter
public class MonthlyClosure {

    private final UUID id;
    private final int month;
    private final int year;
    private final int totalEmployees;
    private final String excelReportUrl;
    private final String pdfReportUrl;
    private final String createdByUserId;
    private final LocalDateTime createdAt;

    public MonthlyClosure(
            UUID id,
            int month,
            int year,
            int totalEmployees,
            String excelReportUrl,
            String pdfReportUrl,
            String createdByUserId,
            LocalDateTime createdAt) {

        if (month < 1 || month > 12)
            throw new IllegalArgumentException("Invalid month");

        if (year < 2000)
            throw new IllegalArgumentException("Invalid year");

        if (totalEmployees < 0)
            throw new IllegalArgumentException("Total employees cannot be negative");

        this.id = Objects.requireNonNull(id);
        this.month = month;
        this.year = year;
        this.totalEmployees = totalEmployees;
        this.excelReportUrl = excelReportUrl;
        this.pdfReportUrl = pdfReportUrl;
        this.createdByUserId = Objects.requireNonNull(createdByUserId);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public MonthlyClosure withReportUrls(String excelReportUrl, String pdfReportUrl) {
        return new MonthlyClosure(
                this.id, this.month, this.year, this.totalEmployees,
                excelReportUrl, pdfReportUrl, this.createdByUserId, this.createdAt);
    }
}