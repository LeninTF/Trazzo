package trazzo.back.corehr.application.dto.command;

import java.time.LocalTime;

public record CreateScheduleCommand(Long shiftId, String name, String description,
                                    LocalTime entryTime, LocalTime departureTime) {
}
