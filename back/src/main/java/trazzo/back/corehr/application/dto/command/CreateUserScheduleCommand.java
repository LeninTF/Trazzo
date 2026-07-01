package trazzo.back.corehr.application.dto.command;

import java.time.LocalTime;

public record CreateUserScheduleCommand(Long tenantUserId, Long scheduleId, String description,
                                        LocalTime entryTime, LocalTime departureTime) {
}
