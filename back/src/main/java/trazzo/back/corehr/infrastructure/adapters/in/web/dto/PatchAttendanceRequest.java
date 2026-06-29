package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import trazzo.back.corehr.domain.model.AttendanceState;

import java.time.LocalDateTime;

public record PatchAttendanceRequest(
        @JsonProperty("check_in") LocalDateTime checkIn,
        @JsonProperty("check_out") LocalDateTime checkOut,
        AttendanceState state,
        @Min(0) @JsonProperty("minutes_late") Integer minutesLate
) {
}
