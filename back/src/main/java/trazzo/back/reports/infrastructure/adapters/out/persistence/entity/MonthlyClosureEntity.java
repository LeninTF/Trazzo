package trazzo.back.reports.infrastructure.adapters.out.persistence.entity;

import java.time.LocalDateTime;
import java.util.UUID;

public class MonthlyClosureEntity {

    private UUID id;
    private int month;
    private int year;
    private int totalEmployees;
    private String excelReportUrl;
    private String pdfReportUrl;
    private UUID createdByUserId;
    private LocalDateTime createdAt;

    public MonthlyClosureEntity() {
    }

    @SuppressWarnings("java:S107")
    public MonthlyClosureEntity(UUID id, int month, int year, int totalEmployees,
                                 String excelReportUrl, String pdfReportUrl,
                                 UUID createdByUserId, LocalDateTime createdAt) {
        this.id = id;
        this.month = month;
        this.year = year;
        this.totalEmployees = totalEmployees;
        this.excelReportUrl = excelReportUrl;
        this.pdfReportUrl = pdfReportUrl;
        this.createdByUserId = createdByUserId;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getTotalEmployees() {
        return totalEmployees;
    }

    public void setTotalEmployees(int totalEmployees) {
        this.totalEmployees = totalEmployees;
    }

    public String getExcelReportUrl() {
        return excelReportUrl;
    }

    public void setExcelReportUrl(String excelReportUrl) {
        this.excelReportUrl = excelReportUrl;
    }

    public String getPdfReportUrl() {
        return pdfReportUrl;
    }

    public void setPdfReportUrl(String pdfReportUrl) {
        this.pdfReportUrl = pdfReportUrl;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(UUID createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
