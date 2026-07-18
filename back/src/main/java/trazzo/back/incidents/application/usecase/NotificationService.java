package trazzo.back.incidents.application.usecase;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import trazzo.back.incidents.application.dto.command.NotifyIncidentCommand;
import trazzo.back.incidents.application.port.in.NotificationUseCase;
import trazzo.back.incidents.application.port.out.IncidentRepositoryPort;

@RequiredArgsConstructor
public class NotificationService implements NotificationUseCase {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final IncidentRepositoryPort incidentRepository;

    @Override
    public void notify(String incidentId, NotifyIncidentCommand command) {
        incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Incidencia no encontrada"));
        // TODO: implement notification logic (email, push, etc.)
        log.info("Notificación enviada para incidencia {}", incidentId);
    }

    @Override
    public void justifyAttendance(String incidentId) {
        incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Incidencia no encontrada"));
        // TODO: implement attendance justification logic
        log.info("Justificación de asistencia registrada para incidencia {}", incidentId);
    }
}
