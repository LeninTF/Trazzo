package trazzo.back.corehr.application.dto.command;

import java.time.LocalDate;

public record PatchNonWorkingDayCommand(LocalDate date, String description, Boolean isRecurring) {
}
