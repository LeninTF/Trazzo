package trazzo.back.reports.application.dto.command;

public record ListMonthlyClosuresCommand(
    Integer year,
    Integer month
) {}