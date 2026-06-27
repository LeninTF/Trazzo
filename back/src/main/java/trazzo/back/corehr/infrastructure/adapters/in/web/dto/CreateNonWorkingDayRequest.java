package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateNonWorkingDayRequest(
        @NotNull LocalDate date,
        String description,
        @JsonProperty("is_recurring") boolean isRecurring
) {
}
