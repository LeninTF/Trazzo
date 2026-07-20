package trazzo.back.incidents.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import trazzo.back.incidents.application.dto.command.NotifyIncidentCommand;
import trazzo.back.incidents.application.port.out.IncidentRepositoryPort;
import trazzo.back.incidents.domain.model.Incident;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

class NotificationServiceTest {

    private IncidentRepositoryPort incidentRepo;
    private NotificationService service;

    @BeforeEach
    void setUp() {
        incidentRepo = mock(IncidentRepositoryPort.class);
        service = new NotificationService(incidentRepo);
    }

    @Test
    void notifyWithExistingIncident() {
        var incident = Incident.restore("inc-1", "u-1", "t-1",
                trazzo.back.incidents.domain.model.IncidentState.PENDIENTE,
                null, null, null, null, List.of(),
                LocalDateTime.now(), LocalDateTime.now());
        when(incidentRepo.findById("inc-1")).thenReturn(Optional.of(incident));

        var command = new NotifyIncidentCommand("JUSTIFICACION");
        assertThrows(UnsupportedOperationException.class, () -> service.notify("inc-1", command));
    }

    @Test
    void notifyWithNonExistentIncidentThrowsException() {
        when(incidentRepo.findById("bad-id")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> service.notify("bad-id", new NotifyIncidentCommand("JUSTIFICACION")));
    }

    @Test
    void justifyAttendanceWithExistingIncident() {
        var incident = Incident.restore("inc-1", "u-1", "t-1",
                trazzo.back.incidents.domain.model.IncidentState.PENDIENTE,
                null, null, null, null, List.of(),
                LocalDateTime.now(), LocalDateTime.now());
        when(incidentRepo.findById("inc-1")).thenReturn(Optional.of(incident));

        assertThrows(UnsupportedOperationException.class, () -> service.justifyAttendance("inc-1"));
    }

    @Test
    void justifyAttendanceWithNonExistentIncidentThrowsException() {
        when(incidentRepo.findById("bad-id")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> service.justifyAttendance("bad-id"));
    }
}
