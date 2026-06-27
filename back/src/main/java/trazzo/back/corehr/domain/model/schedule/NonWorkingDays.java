package trazzo.back.corehr.domain.model.schedule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import trazzo.back.corehr.domain.exception.InvalidNonWorkingDaysException;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NonWorkingDays {

    private Long id;
    private LocalDate date;
    private String description;
    private boolean isRecurring;
    private LocalDateTime createdAt;

    private NonWorkingDays(Long id, LocalDate date, String description, boolean isRecurring, LocalDateTime createdAt) {
        this.id = id;
        this.date = requireDate(date);
        this.description = normalizeOptionalText(description);
        this.isRecurring = isRecurring;
        this.createdAt = createdAt;
    }

    public static NonWorkingDays create(LocalDate date, String description, boolean isRecurring) {
        return new NonWorkingDays(null, date, description, isRecurring, LocalDateTime.now());
    }

    public static NonWorkingDays restore(Long id, LocalDate date, String description, boolean isRecurring, LocalDateTime createdAt) {
        return new NonWorkingDays(id, date, description, isRecurring, createdAt);
    }

    public void updateDescription(String description) {
        this.description = normalizeOptionalText(description);
    }

    private static LocalDate requireDate(LocalDate date) {
        if (date == null) {
            throw new InvalidNonWorkingDaysException("date is required");
        }
        return date;
    }

    private static String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
