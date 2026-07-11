package trazzo.back.incidents.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import trazzo.back.incidents.application.dto.command.PatchIncidentCommand;
import trazzo.back.incidents.application.dto.command.IncidentStateChangeCommand;
import trazzo.back.incidents.application.port.out.EventPublisherPort;
import trazzo.back.incidents.application.port.out.IncidentRepositoryPort;
import trazzo.back.incidents.application.port.out.IncidentTypeRepositoryPort;
import trazzo.back.corehr.application.port.out.TenantUserPort;
import trazzo.back.incidents.domain.model.Incident;
import trazzo.back.incidents.domain.model.IncidentState;
import trazzo.back.shared.application.port.out.FileStoragePort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

class IncidentServiceMoreTest {

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
    void patchWithNonExistentIncidentThrowsException() {
        when(incidentRepo.findById("bad-id")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> service.patch("bad-id", new PatchIncidentCommand("nuevo")));
    }

    @Test
    void changeStateWithInvalidStateThrowsException() {
        var now = LocalDateTime.now();
        var incident = Incident.restore("inc-1", "u-1", "t-1", IncidentState.PENDIENTE,
                "comment", null, null, null, List.of(), now, now);
        when(incidentRepo.findById("inc-1")).thenReturn(Optional.of(incident));
        when(incidentRepo.save(any())).thenAnswer(invocation -> invocation.<Incident>getArgument(0));

        assertThrows(IllegalArgumentException.class,
                () -> service.changeState("inc-1", new IncidentStateChangeCommand(null, null, null)));
    }

    @Test
    void changeStateWithNonExistentIncidentThrowsException() {
        when(incidentRepo.findById("bad-id")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> service.changeState("bad-id", new IncidentStateChangeCommand(IncidentState.APROBADO, null, null)));
    }

    @Test
    void findByIdReturnsEmptyWhenNotFound() {
        when(incidentRepo.findById("bad-id")).thenReturn(Optional.empty());
        assertTrue(service.findById("bad-id").isEmpty());
    }

    @Test
    void findAllReturnsPaginatedResults() {
        var now = LocalDateTime.now();
        var incident = Incident.restore("inc-1", "u-1", "t-1", IncidentState.PENDIENTE,
                "comment", null, null, null, List.of(), now, now);
        when(incidentRepo.findAll(any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), any()))
                .thenReturn(List.of(incident));
        when(incidentRepo.count(any(), any(), any(), any(), any(), any())).thenReturn(1L);

        var result = service.findAll(null, null, null, null, null, null, null, null, null, 0, 20, null);

        assertEquals(1, result.content().size());
        assertEquals(0, result.page());
        assertEquals(1, result.totalElements());
    }

    @Test
    void toResultWithTypeAndPermissionAndUser() {
        var now = LocalDateTime.now();
        var type = trazzo.back.incidents.domain.model.IncidentType.restore("t-1", "Permiso", "Desc", true, now, now);
        var permission = trazzo.back.incidents.domain.model.IncidentPermission.create("inc-1",
                java.time.LocalDate.now(), java.time.LocalDate.now().plusDays(1), 1);
        var evidence = trazzo.back.incidents.domain.model.IncidentEvidence.create("inc-1", "doc.pdf", "http://url", "pdf", 100);
        var incident = Incident.restore("inc-1", "1", "t-1", IncidentState.PENDIENTE,
                "comment", null, type, permission, List.of(evidence), now, now);
        when(incidentRepo.findById("inc-1")).thenReturn(Optional.of(incident));
        var userInfo = new trazzo.back.corehr.application.port.out.TenantUserPort.TenantUserBasicInfo(
                1L, "Juan", "Perez", "Lopez", "juan@mail.com", "999888777");
        when(tenantUserPort.findBasicInfoById(1L)).thenReturn(Optional.of(userInfo));

        var result = service.findById("inc-1");

        assertTrue(result.isPresent());
        assertEquals("Permiso", result.get().tipo().nombre());
        assertEquals(1, result.get().permiso().daysGranted());
        assertEquals(1, result.get().evidencias().size());
        assertEquals("Juan", result.get().tenantUser().nombre());
    }
}
