package trazzo.back.incidents.application.usecase;

import lombok.RequiredArgsConstructor;
import trazzo.back.incidents.application.dto.command.NotifyIncidentCommand;
import trazzo.back.incidents.application.port.in.NotificationUseCase;
import trazzo.back.incidents.application.port.out.IncidentRepositoryPort;

@RequiredArgsConstructor
public class NotificationService implements NotificationUseCase {

    private final IncidentRepositoryPort incidentRepository;

    @Override
    public void notify(String incidentId, NotifyIncidentCommand command) {
        incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Incidencia no encontrada"));
        throw new UnsupportedOperationException("Notification logic not yet implemented");
    }

    @Override
    public void justifyAttendance(String incidentId) {
        incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Incidencia no encontrada"));
        throw new UnsupportedOperationException("Attendance justification logic not yet implemented");
    }
}
