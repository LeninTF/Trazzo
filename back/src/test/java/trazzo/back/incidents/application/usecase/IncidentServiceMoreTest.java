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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

class IncidentServiceMoreTest {

    private IncidentRepositoryPort incidentRepo;
    private IncidentTypeRepositoryPort typeRepo;
    private TenantUserPort tenantUserPort;
    private EventPublisherPort eventPublisher;
    private IncidentService service;

    @BeforeEach
    void setUp() {
        incidentRepo = mock(IncidentRepositoryPort.class);
        typeRepo = mock(IncidentTypeRepositoryPort.class);
        tenantUserPort = mock(TenantUserPort.class);
        eventPublisher = mock(EventPublisherPort.class);
        service = new IncidentService(incidentRepo, typeRepo, tenantUserPort, eventPublisher);
    }

    @Test
    void patchWithNonExistentIncidentThrowsException() {
        when(incidentRepo.findById(999)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> service.patch(999, new PatchIncidentCommand("nuevo")));
    }

    @Test
    void changeStateWithInvalidStateThrowsException() {
        var now = LocalDateTime.now();
        var incident = Incident.restore(1, 1, 1, IncidentState.PENDIENTE,
                "comment", null, null, null, List.of(), now, now);
        when(incidentRepo.findById(1)).thenReturn(Optional.of(incident));
        when(incidentRepo.save(any())).thenAnswer(invocation -> invocation.<Incident>getArgument(0));

        assertThrows(IllegalArgumentException.class,
                () -> service.changeState(1, new IncidentStateChangeCommand(null, null, null)));
    }

    @Test
    void changeStateWithNonExistentIncidentThrowsException() {
        when(incidentRepo.findById(999)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> service.changeState(999, new IncidentStateChangeCommand(IncidentState.APROBADO, null, null)));
    }

    @Test
    void findByIdReturnsEmptyWhenNotFound() {
        when(incidentRepo.findById(999)).thenReturn(Optional.empty());
        assertTrue(service.findById(999).isEmpty());
    }

    @Test
    void findAllReturnsPaginatedResults() {
        var now = LocalDateTime.now();
        var incident = Incident.restore(1, 1, 1, IncidentState.PENDIENTE,
                "comment", null, null, null, List.of(), now, now);
        when(incidentRepo.findAll(any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), any()))
                .thenReturn(List.of(incident));
        when(incidentRepo.count(any(), any(), any(), any(), any(), any())).thenReturn(1L);

        var result = service.findAll(null, null, null, null, null, null, null, null, null, null, 0, 20, null);

        assertEquals(1, result.content().size());
        assertEquals(0, result.page());
        assertEquals(1, result.totalElements());
    }

    @Test
    void findAllScopeSelfPassesTenantUserIdToRepo() {
        var now = LocalDateTime.now();
        var incident = Incident.restore(1, 42, 1, IncidentState.PENDIENTE,
                "comment", null, null, null, List.of(), now, now);
        when(incidentRepo.findAll(eq(42), any(), any(), any(), any(), any(), anyInt(), anyInt(), any()))
                .thenReturn(List.of(incident));
        when(incidentRepo.count(eq(42), any(), any(), any(), any(), any())).thenReturn(1L);

        var result = service.findAll(42, "SELF", null, null, null, null, null, null, null, null, 0, 20, null);

        assertEquals(1, result.content().size());
        verify(incidentRepo).findAll(eq(42), isNull(), isNull(), isNull(), isNull(), isNull(), eq(0), eq(20), isNull());
        verify(incidentRepo).count(eq(42), isNull(), isNull(), isNull(), isNull(), isNull());
    }

    @Test
    void findAllScopeAllPassesNullTenantUserIdToRepo() {
        var now = LocalDateTime.now();
        var incident = Incident.restore(1, 1, 1, IncidentState.PENDIENTE,
                "comment", null, null, null, List.of(), now, now);
        when(incidentRepo.findAll(isNull(), any(), any(), any(), any(), any(), anyInt(), anyInt(), any()))
                .thenReturn(List.of(incident));
        when(incidentRepo.count(isNull(), any(), any(), any(), any(), any())).thenReturn(1L);

        service.findAll(null, "ALL", null, null, null, null, null, null, null, null, 0, 20, null);

        verify(incidentRepo).findAll(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(0), eq(20), isNull());
    }

    @Test
    void findAllScopeSelfWithNullTenantUserIdFailsClosed() {
        var ex = assertThrows(IllegalStateException.class,
                () -> service.findAll(null, "SELF", null, null, null, null, null, null, null, null, 0, 20, null));
        assertTrue(ex.getMessage().contains("scope=SELF"));
        verifyNoInteractions(incidentRepo);
    }

    @Test
    void toResultWithTypeAndPermissionAndUser() {
        var now = LocalDateTime.now();
        var type = trazzo.back.incidents.domain.model.IncidentType.restore(1, "Permiso", "Desc", true, now, now);
        var permission = trazzo.back.incidents.domain.model.IncidentPermission.create(1,
                java.time.LocalDate.now(), java.time.LocalDate.now().plusDays(1), 1);
        var evidence = trazzo.back.incidents.domain.model.IncidentEvidence.restore(1, 1, "doc.pdf", "http://url", "pdf", 100, false, null, now, now, now);
        var incident = Incident.restore(1, 1, 1, IncidentState.PENDIENTE,
                "comment", null, type, permission, List.of(evidence), now, now);
        when(incidentRepo.findById(1)).thenReturn(Optional.of(incident));
        var userInfo = new trazzo.back.corehr.application.port.out.TenantUserPort.TenantUserBasicInfo(
                1L, "Juan", "Perez", "Lopez", "juan@mail.com", "999888777");
        when(tenantUserPort.findBasicInfoById(1L)).thenReturn(Optional.of(userInfo));

        var result = service.findById(1);

        assertTrue(result.isPresent());
        assertEquals("Permiso", result.get().tipo().nombre());
        assertEquals(1, result.get().permiso().daysGranted());
        assertEquals(1, result.get().evidencias().size());
        assertEquals("Juan", result.get().tenantUser().nombre());
        var ev = result.get().evidencias().get(0);
        assertEquals("/api/v1/incidentes/1/evidencias/1/descarga", ev.downloadUrl());
        assertTrue(ev.downloadUrl().endsWith("/descarga"));
    }
}
