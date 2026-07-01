package trazzo.back.corehr.application.dto.command;

import java.time.LocalDate;

public record CreateNonWorkingDayCommand(LocalDate date, String description, boolean isRecurring) {
}
