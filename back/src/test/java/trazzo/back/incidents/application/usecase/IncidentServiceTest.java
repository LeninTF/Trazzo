package trazzo.back.incidents.application.usecase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import trazzo.back.corehr.application.port.out.TenantUserPort;
import trazzo.back.incidents.application.dto.command.CreateIncidentCommand;
import trazzo.back.incidents.application.dto.command.IncidentStateChangeCommand;
import trazzo.back.incidents.application.dto.command.PatchIncidentCommand;
import trazzo.back.incidents.application.port.out.EventPublisherPort;
import trazzo.back.incidents.application.port.out.EvidenceUrlResolver;
import trazzo.back.incidents.application.port.out.IncidentRepositoryPort;
import trazzo.back.incidents.application.port.out.IncidentTypeRepositoryPort;
import trazzo.back.incidents.domain.model.Incident;
import trazzo.back.incidents.domain.model.IncidentState;
import trazzo.back.incidents.domain.model.IncidentType;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock
    private IncidentRepositoryPort incidentRepository;

    @Mock
    private IncidentTypeRepositoryPort typeRepository;

    @Mock
    private TenantUserPort tenantUserPort;

    @Mock
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
    void createWithValidCommand() {
        var now = LocalDateTime.now();
        var type = IncidentType.restore(1, "Permiso", "Desc", true, now, now);
        when(typeRepo.findById(1)).thenReturn(Optional.of(type));
        when(incidentRepo.save(any())).thenAnswer(invocation -> invocation.<Incident>getArgument(0));

        var command = new CreateIncidentCommand(1, 1, "comment");
        var result = service.create(command);

        assertEquals(1, result.tenantUserId());
        assertEquals(IncidentState.PENDIENTE, result.state());
        assertEquals("comment", result.comment());
        assertNotNull(result.tipo());
        verify(eventPublisher).publish(any());
    }

    @Test
    void createWithInactiveTypeThrowsException() {
        var now = LocalDateTime.now();
        var type = IncidentType.restore(1, "Permiso", "Desc", false, now, now);
        when(typeRepo.findById(1)).thenReturn(Optional.of(type));

        var command = new CreateIncidentCommand(1, 1, "comment");

        assertThrows(IllegalStateException.class, () -> service.create(command));
        verify(incidentRepo, never()).save(any());
    }

    @Test
    void createWithNonExistentTypeThrowsException() {
        when(typeRepo.findById(999)).thenReturn(Optional.empty());

        var command = new CreateIncidentCommand(1, 999, "comment");

        assertThrows(IllegalArgumentException.class, () -> service.create(command));
    }

    @Test
    void findByIdReturnsIncident() {
        var now = LocalDateTime.now();
        var incident = Incident.restore(1, 1, 1, IncidentState.PENDIENTE,
                "comment", null, null, null, java.util.List.of(), now, now);
        when(incidentRepo.findById(1)).thenReturn(Optional.of(incident));

        var result = service.findById(1);

        assertTrue(result.isPresent());
        assertEquals(1, result.get().id());
    }

    @Test
    void patchUpdatesComment() {
        var now = LocalDateTime.now();
        var incident = Incident.restore(1, 1, 1, IncidentState.PENDIENTE,
                "original", null, null, null, java.util.List.of(), now, now);
        when(incidentRepo.findById(1)).thenReturn(Optional.of(incident));
        when(incidentRepo.save(any())).thenAnswer(invocation -> invocation.<Incident>getArgument(0));

        var command = new PatchIncidentCommand("modificado");
        var result = service.patch(1, command);

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void changeStateToApproved() {
        var now = LocalDateTime.now();
        var incident = Incident.restore(1, 1, 1, IncidentState.PENDIENTE,
                "comment", null, null, null, java.util.List.of(), now, now);
        when(incidentRepo.findById(1)).thenReturn(Optional.of(incident));
        when(incidentRepo.save(any())).thenAnswer(invocation -> invocation.<Incident>getArgument(0));

        var command = new IncidentStateChangeCommand(IncidentState.APROBADO, null, null);
        var result = service.changeState(1, command);

        assertThat(result.comment()).isEqualTo("Updated comment");
    }

    @Test
    void changeStateToApprovedWithPermission() {
        var now = LocalDateTime.now();
        var incident = Incident.restore(1, 1, 1, IncidentState.PENDIENTE,
                "comment", null, null, null, java.util.List.of(), now, now);
        when(incidentRepo.findById(1)).thenReturn(Optional.of(incident));
        when(incidentRepo.save(any())).thenAnswer(invocation -> invocation.<Incident>getArgument(0));

        var command = new IncidentStateChangeCommand(IncidentState.APROBADO, 3, null);
        var result = service.changeState(1, command);

        assertEquals(IncidentState.APROBADO, result.state());
        assertNotNull(result.permiso());
        assertEquals(3, result.permiso().daysGranted());
    }

    @Test
    void changeStateToDenied() {
        var now = LocalDateTime.now();
        var incident = Incident.restore(1, 1, 1, IncidentState.PENDIENTE,
                "comment", null, null, null, java.util.List.of(), now, now);
        when(incidentRepo.findById(1)).thenReturn(Optional.of(incident));
        when(incidentRepo.save(any())).thenAnswer(invocation -> invocation.<Incident>getArgument(0));

        var command = new IncidentStateChangeCommand(IncidentState.DENEGADO, null, "motivo");
        var result = service.changeState(1, command);

        assertThat(result.state()).isEqualTo(IncidentState.APROBADO);
    }

    @Test
    void changeState_approveWithPermission_shouldSetApproved() {
        var cmd = new IncidentStateChangeCommand(IncidentState.APROBADO, 5, null);
        when(incidentRepository.findById("inc-1")).thenReturn(Optional.of(sampleIncident()));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(i -> i.getArgument(0));

        var result = service.changeState("inc-1", cmd);

        assertThat(result.state()).isEqualTo(IncidentState.APROBADO);
        assertThat(result.permiso()).isNotNull();
    }

    @Test
    void changeState_deny_shouldSetDenied() {
        var cmd = new IncidentStateChangeCommand(IncidentState.DENEGADO, null, "Invalid reason");
        when(incidentRepository.findById("inc-1")).thenReturn(Optional.of(sampleIncident()));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(i -> i.getArgument(0));

        var result = service.changeState("inc-1", cmd);

        assertThat(result.state()).isEqualTo(IncidentState.DENEGADO);
    }

    @Test
    void changeState_shouldThrowWhenNotFound() {
        var cmd = new IncidentStateChangeCommand(IncidentState.APROBADO, null, null);
        when(incidentRepository.findById("bad-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeState("bad-id", cmd))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void changeState_shouldThrowWhenInvalidState() {
        var cmd = new IncidentStateChangeCommand(IncidentState.PENDIENTE, null, null);
        when(incidentRepository.findById("inc-1")).thenReturn(Optional.of(sampleIncident()));

        assertThatThrownBy(() -> service.changeState("inc-1", cmd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Estado no válido");
    }

    @Test
    void findAll_attachTypes_batchOnly() {
        var incident1 = Incident.restore("inc-1", "user-1", "type-1",
                IncidentState.PENDIENTE, "c1", null, null, null,
                Collections.emptyList(), LocalDateTime.now(), LocalDateTime.now());
        var incident2 = Incident.restore("inc-2", "user-2", "type-missing",
                IncidentState.PENDIENTE, "c2", null, null, null,
                Collections.emptyList(), LocalDateTime.now(), LocalDateTime.now());

        when(incidentRepository.findAll(null, null, null, null, null, null, 0, 10, null))
                .thenReturn(List.of(incident1, incident2));
        when(incidentRepository.count(null, null, null, null, null, null)).thenReturn(2L);
        when(typeRepository.findByIdIn(List.of("type-1", "type-missing")))
                .thenReturn(List.of(sampleType()));

        var result = service.findAll(null, null, null, null, null, null,
                null, null, null, 0, 10, null);

        assertThat(result.content()).hasSize(2);
        verify(typeRepository).findByIdIn(anyList());
        verify(typeRepository, never()).findById(any());
    }

    @Test
    void findAll_handlesNullTypeIds() {
        var incident = Incident.restore("inc-1", "user-1", "type-x",
                IncidentState.PENDIENTE, "c", null, null, null,
                Collections.emptyList(), LocalDateTime.now(), LocalDateTime.now());

        when(incidentRepository.findAll(null, null, null, null, null, null, 0, 10, null))
                .thenReturn(List.of(incident));
        when(incidentRepository.count(null, null, null, null, null, null)).thenReturn(1L);
        when(typeRepository.findByIdIn(List.of("type-x"))).thenReturn(List.of());

        var result = service.findAll(null, null, null, null, null, null,
                null, null, null, 0, 10, null);

        assertThat(result.content()).hasSize(1);
    }

    @Test
    void toResult_includesTenantUserInfo() {
        var incident = Incident.restore("inc-1", "123", "type-1",
                IncidentState.PENDIENTE, "comment", null, null, null,
                Collections.emptyList(), LocalDateTime.now(), LocalDateTime.now());
        when(incidentRepository.findById("inc-1")).thenReturn(Optional.of(incident));
        when(tenantUserPort.findBasicInfoById(123L)).thenReturn(
                Optional.of(new trazzo.back.corehr.application.port.out.TenantUserPort.TenantUserBasicInfo(
                        123L, "Juan", "Perez", "Lopez", "juan@test.com", "5551234")));

        var result = service.findById("inc-1");

        assertThat(result).isPresent();
        assertThat(result.get().tenantUser()).isNotNull();
        assertThat(result.get().tenantUser().nombre()).isEqualTo("Juan");
    }

    @Test
    void toResult_handlesInvalidTenantUserId() {
        var incident = Incident.restore("inc-1", "not-a-number", "type-1",
                IncidentState.PENDIENTE, "comment", null, null, null,
                Collections.emptyList(), LocalDateTime.now(), LocalDateTime.now());
        when(incidentRepository.findById("inc-1")).thenReturn(Optional.of(incident));

        var result = service.findById("inc-1");

        assertThat(result).isPresent();
        assertThat(result.get().tenantUser()).isNull();
    }

    @Test
    void changeState_approveWithZeroDays_setsApprovedWithoutPermission() {
        var cmd = new IncidentStateChangeCommand(IncidentState.APROBADO, 0, null);
        when(incidentRepository.findById("inc-1")).thenReturn(Optional.of(sampleIncident()));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(i -> i.getArgument(0));

        var result = service.changeState("inc-1", cmd);

        assertThat(result.state()).isEqualTo(IncidentState.APROBADO);
        assertThat(result.permiso()).isNull();
    }

    @Test
    void toResult_includesEvidenceWithUrl() {
        var incident = Incident.restore("inc-1", "user-1", "type-1",
                IncidentState.PENDIENTE, "comment", null, null, null,
                Collections.emptyList(), LocalDateTime.now(), LocalDateTime.now());
        incident.addEvidence(trazzo.back.incidents.domain.model.IncidentEvidence.create(
                "inc-1", "file.pdf", "key-1", "application/pdf", 1024));

        when(incidentRepository.findById("inc-1")).thenReturn(Optional.of(incident));
        when(evidenceUrlResolver.buildPublicUrl("key-1")).thenReturn("http://storage/key-1");

        var result = service.findById("inc-1");

        assertThat(result).isPresent();
        assertThat(result.get().evidencias()).hasSize(1);
        assertThat(result.get().evidencias().get(0).fileUrl()).isEqualTo("http://storage/key-1");
    }
}
