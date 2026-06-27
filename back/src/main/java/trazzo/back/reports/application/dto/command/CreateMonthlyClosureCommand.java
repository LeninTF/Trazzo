package trazzo.back.reports.application.dto.command;

public record CreateMonthlyClosureCommand(
        int month,
        int year,
        String createdByUserId) {
}
