package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import trazzo.back.corehr.application.dto.result.NonWorkingDayResult;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record NonWorkingDayResponse(
        Long id,
        LocalDate date,
        String description,
        @JsonProperty("is_recurring") boolean isRecurring,
        @JsonProperty("created_at") LocalDateTime createdAt
) {
    public static NonWorkingDayResponse from(NonWorkingDayResult result) {
        return new NonWorkingDayResponse(result.id(), result.date(), result.description(),
                result.isRecurring(), result.createdAt());
    }
}
