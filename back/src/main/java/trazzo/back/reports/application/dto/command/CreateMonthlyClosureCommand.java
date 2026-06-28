package trazzo.back.reports.application.dto.command;

import java.util.UUID;

public record CreateMonthlyClosureCommand(
        int month,
        int year,
        UUID createdByUserId) {
}
