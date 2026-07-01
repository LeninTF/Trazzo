package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public record PatchNonWorkingDayRequest(
        LocalDate date,
        String description,
        @JsonProperty("is_recurring") Boolean isRecurring
) {
}
