package trazzo.back.incidents.application.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import trazzo.back.incidents.application.dto.command.NotifyIncidentCommand;
import trazzo.back.incidents.application.port.in.NotificationUseCase;
import trazzo.back.incidents.application.port.out.IncidentRepositoryPort;

@RequiredArgsConstructor
@Service
public class NotificationService implements NotificationUseCase {

    private final IncidentRepositoryPort incidentRepository;

    @Override
    public void notify(String incidentId, NotifyIncidentCommand command) {
        var incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Incidencia no encontrada: " + incidentId));
        if (incident == null) {
            return;
        }
    }

    @Override
    public void justifyAttendance(String incidentId) {
        var incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Incidencia no encontrada: " + incidentId));
        if (incident == null) {
            return;
        }
    }
}
