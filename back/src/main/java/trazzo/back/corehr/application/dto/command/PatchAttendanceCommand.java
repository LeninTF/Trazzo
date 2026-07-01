package trazzo.back.corehr.application.dto.command;

import trazzo.back.corehr.domain.model.AttendanceState;

import java.time.LocalDateTime;

public record PatchAttendanceCommand(LocalDateTime checkIn, LocalDateTime checkOut,
                                     AttendanceState state, Integer minutesLate) {
}
