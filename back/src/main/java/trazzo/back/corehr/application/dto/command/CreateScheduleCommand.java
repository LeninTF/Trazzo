package trazzo.back.corehr.application.dto.command;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

public record CreateScheduleCommand(
        Long shiftId,
        String name,
        String description,
        LocalTime entryTime,
        LocalTime departureTime,
        List<DayOfWeek> daysOfWeek
) {
}
