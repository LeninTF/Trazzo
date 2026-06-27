package trazzo.back.reports.infrastructure.adapters.in.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateMonthlyClosureRequest(
        @Min(1) @Max(12) int month,
        @Min(2000) int year,
        @NotNull UUID createdByUserId) {
}
