package trazzo.back.incidents.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import trazzo.back.incidents.application.dto.command.CreateIncidentCommand;
import trazzo.back.incidents.application.dto.command.IncidentStateChangeCommand;
import trazzo.back.incidents.application.dto.command.PatchIncidentCommand;
import trazzo.back.incidents.application.port.out.EventPublisherPort;
import trazzo.back.incidents.application.port.out.IncidentRepositoryPort;
import trazzo.back.incidents.application.port.out.IncidentTypeRepositoryPort;
import trazzo.back.corehr.application.port.out.TenantUserPort;
import trazzo.back.incidents.domain.model.Incident;
import trazzo.back.incidents.domain.model.IncidentState;
import trazzo.back.incidents.domain.model.IncidentType;
import trazzo.back.shared.application.port.out.FileStoragePort;

import java.time.LocalDateTime;
import java.util.Optional;

class IncidentServiceTest {

    private IncidentRepositoryPort incidentRepo;
    private IncidentTypeRepositoryPort typeRepo;
    private TenantUserPort tenantUserPort;
    private EventPublisherPort eventPublisher;
    private FileStoragePort fileStoragePort;
    private IncidentService service;

    @BeforeEach
    void setUp() {
        incidentRepo = mock(IncidentRepositoryPort.class);
        typeRepo = mock(IncidentTypeRepositoryPort.class);
        tenantUserPort = mock(TenantUserPort.class);
        eventPublisher = mock(EventPublisherPort.class);
        fileStoragePort = mock(FileStoragePort.class);
        when(fileStoragePort.buildPublicUrl(any())).thenReturn("http://public-url/test");
        service = new IncidentService(incidentRepo, typeRepo, tenantUserPort, eventPublisher, fileStoragePort);
    }

    @Test
    void createWithValidCommand() {
        var now = LocalDateTime.now();
        var type = IncidentType.restore("t-1", "Permiso", "Desc", true, now, now);
        when(typeRepo.findById("t-1")).thenReturn(Optional.of(type));
        when(incidentRepo.save(any())).thenAnswer(invocation -> invocation.<Incident>getArgument(0));

        var command = new CreateIncidentCommand("u-1", "t-1", "comment");
        var result = service.create(command);

        assertEquals("u-1", result.tenantUserId());
        assertEquals(IncidentState.PENDIENTE, result.state());
        assertEquals("comment", result.comment());
        assertNotNull(result.tipo());
        verify(eventPublisher).publish(any());
    }

    @Test
    void createWithInactiveTypeThrowsException() {
        var now = LocalDateTime.now();
        var type = IncidentType.restore("t-1", "Permiso", "Desc", false, now, now);
        when(typeRepo.findById("t-1")).thenReturn(Optional.of(type));

        var command = new CreateIncidentCommand("u-1", "t-1", "comment");

        assertThrows(IllegalStateException.class, () -> service.create(command));
        verify(incidentRepo, never()).save(any());
    }

    @Test
    void createWithNonExistentTypeThrowsException() {
        when(typeRepo.findById("bad-type")).thenReturn(Optional.empty());

        var command = new CreateIncidentCommand("u-1", "bad-type", "comment");

        assertThrows(IllegalArgumentException.class, () -> service.create(command));
    }

    @Test
    void findByIdReturnsIncident() {
        var now = LocalDateTime.now();
        var incident = Incident.restore("inc-1", "u-1", "t-1", IncidentState.PENDIENTE,
                "comment", null, null, null, java.util.List.of(), now, now);
        when(incidentRepo.findById("inc-1")).thenReturn(Optional.of(incident));

        var result = service.findById("inc-1");

        assertTrue(result.isPresent());
        assertEquals("inc-1", result.get().id());
    }

    @Test
    void patchUpdatesComment() {
        var now = LocalDateTime.now();
        var incident = Incident.restore("inc-1", "u-1", "t-1", IncidentState.PENDIENTE,
                "original", null, null, null, java.util.List.of(), now, now);
        when(incidentRepo.findById("inc-1")).thenReturn(Optional.of(incident));
        when(incidentRepo.save(any())).thenAnswer(invocation -> invocation.<Incident>getArgument(0));

        var command = new PatchIncidentCommand("modificado");
        var result = service.patch("inc-1", command);

        assertEquals("modificado", result.comment());
    }

    @Test
    void changeStateToApproved() {
        var now = LocalDateTime.now();
        var incident = Incident.restore("inc-1", "u-1", "t-1", IncidentState.PENDIENTE,
                "comment", null, null, null, java.util.List.of(), now, now);
        when(incidentRepo.findById("inc-1")).thenReturn(Optional.of(incident));
        when(incidentRepo.save(any())).thenAnswer(invocation -> invocation.<Incident>getArgument(0));

        var command = new IncidentStateChangeCommand(IncidentState.APROBADO, null, null);
        var result = service.changeState("inc-1", command);

        assertEquals(IncidentState.APROBADO, result.state());
        verify(eventPublisher, atLeastOnce()).publish(any());
    }

    @Test
    void changeStateToApprovedWithPermission() {
        var now = LocalDateTime.now();
        var incident = Incident.restore("inc-1", "u-1", "t-1", IncidentState.PENDIENTE,
                "comment", null, null, null, java.util.List.of(), now, now);
        when(incidentRepo.findById("inc-1")).thenReturn(Optional.of(incident));
        when(incidentRepo.save(any())).thenAnswer(invocation -> invocation.<Incident>getArgument(0));

        var command = new IncidentStateChangeCommand(IncidentState.APROBADO, 3, null);
        var result = service.changeState("inc-1", command);

        assertEquals(IncidentState.APROBADO, result.state());
        assertNotNull(result.permiso());
        assertEquals(3, result.permiso().daysGranted());
    }

    @Test
    void changeStateToDenied() {
        var now = LocalDateTime.now();
        var incident = Incident.restore("inc-1", "u-1", "t-1", IncidentState.PENDIENTE,
                "comment", null, null, null, java.util.List.of(), now, now);
        when(incidentRepo.findById("inc-1")).thenReturn(Optional.of(incident));
        when(incidentRepo.save(any())).thenAnswer(invocation -> invocation.<Incident>getArgument(0));

        var command = new IncidentStateChangeCommand(IncidentState.DENEGADO, null, "motivo");
        var result = service.changeState("inc-1", command);

        assertEquals(IncidentState.DENEGADO, result.state());
        assertEquals("motivo", result.rejectionReason());
    }
}
