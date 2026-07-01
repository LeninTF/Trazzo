package trazzo.back.corehr.application.dto.command;

import java.time.LocalTime;

public record PatchScheduleCommand(String name, String description,
                                   LocalTime entryTime, LocalTime departureTime) {
}
