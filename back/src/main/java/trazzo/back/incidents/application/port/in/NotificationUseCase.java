package trazzo.back.incidents.application.port.in;

import trazzo.back.incidents.application.dto.command.NotifyIncidentCommand;

public interface NotificationUseCase {
    void notify(String incidentId, NotifyIncidentCommand command);
    void justifyAttendance(String incidentId);
}
