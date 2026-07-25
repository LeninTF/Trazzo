package trazzo.back.incidents.application.usecase;

import lombok.RequiredArgsConstructor;
import trazzo.back.incidents.application.dto.command.NotifyIncidentCommand;
import trazzo.back.incidents.application.port.in.NotificationUseCase;
import trazzo.back.incidents.application.port.out.IncidentRepositoryPort;

@RequiredArgsConstructor
public class NotificationService implements NotificationUseCase {

    private final IncidentRepositoryPort incidentRepository;

    @Override
    public void notify(Integer incidentId, NotifyIncidentCommand command) {
        var incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Incidencia no encontrada: " + incidentId));
        if (incident == null) {
            return;
        }
    }

    @Override
    public void justifyAttendance(Integer incidentId) {
        var incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Incidencia no encontrada: " + incidentId));
        if (incident == null) {
            return;
        }
    }
}
