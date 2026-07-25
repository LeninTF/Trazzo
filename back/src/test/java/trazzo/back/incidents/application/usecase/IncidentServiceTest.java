package trazzo.back.incidents.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import trazzo.back.corehr.application.port.out.TenantUserPort;
import trazzo.back.incidents.application.dto.command.CreateIncidentCommand;
import trazzo.back.incidents.application.dto.command.IncidentStateChangeCommand;
import trazzo.back.incidents.application.dto.command.PatchIncidentCommand;
import trazzo.back.incidents.application.port.out.EventPublisherPort;
import trazzo.back.incidents.application.port.out.IncidentRepositoryPort;
import trazzo.back.incidents.application.port.out.IncidentTypeRepositoryPort;
import trazzo.back.incidents.domain.model.Incident;
import trazzo.back.incidents.domain.model.IncidentEvidence;
import trazzo.back.incidents.domain.model.IncidentState;
import trazzo.back.incidents.domain.model.IncidentType;

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
        service = new IncidentService(incidentRepository, typeRepository, tenantUserPort, eventPublisher);
    }

    @Test
    void createWithValidCommand() {
        var now = LocalDateTime.now();
        var type = IncidentType.restore(1, "Permiso", "Desc", true, now, now);
        when(typeRepository.findById(1)).thenReturn(Optional.of(type));
        when(incidentRepository.save(any())).thenAnswer(invocation -> invocation.<Incident>getArgument(0));

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
        when(typeRepository.findById(1)).thenReturn(Optional.of(type));

        var command = new CreateIncidentCommand(1, 1, "comment");

        assertThrows(IllegalStateException.class, () -> service.create(command));
        verify(incidentRepository, never()).save(any());
    }

    @Test
    void createWithNonExistentTypeThrowsException() {
        when(typeRepository.findById(999)).thenReturn(Optional.empty());

        var command = new CreateIncidentCommand(1, 999, "comment");

        assertThrows(IllegalArgumentException.class, () -> service.create(command));
    }

    @Test
    void findByIdReturnsIncident() {
        var now = LocalDateTime.now();
        var incident = Incident.restore(1, 1, 1, IncidentState.PENDIENTE,
                "comment", null, null, null, Collections.emptyList(), now, now);
        when(incidentRepository.findById(1)).thenReturn(Optional.of(incident));

        var result = service.findById(1);

        assertTrue(result.isPresent());
        assertEquals(1, result.get().id());
    }

    @Test
    void patchUpdatesComment() {
        var now = LocalDateTime.now();
        var incident = Incident.restore(1, 1, 1, IncidentState.PENDIENTE,
                "original", null, null, null, Collections.emptyList(), now, now);
        when(incidentRepository.findById(1)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(any())).thenAnswer(invocation -> invocation.<Incident>getArgument(0));

        var command = new PatchIncidentCommand("modificado");
        var result = service.patch(1, command);

        assertThat(result.comment()).isEqualTo("modificado");
    }

    @Test
    void changeStateToApproved() {
        var now = LocalDateTime.now();
        var incident = Incident.restore(1, 1, 1, IncidentState.PENDIENTE,
                "comment", null, null, null, Collections.emptyList(), now, now);
        when(incidentRepository.findById(1)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(any())).thenAnswer(invocation -> invocation.<Incident>getArgument(0));

        var command = new IncidentStateChangeCommand(IncidentState.APROBADO, null, null);
        var result = service.changeState(1, command);

        assertThat(result.state()).isEqualTo(IncidentState.APROBADO);
        assertThat(result.permiso()).isNull();
    }

    @Test
    void changeStateToApprovedWithPermission() {
        var now = LocalDateTime.now();
        var incident = Incident.restore(1, 1, 1, IncidentState.PENDIENTE,
                "comment", null, null, null, Collections.emptyList(), now, now);
        when(incidentRepository.findById(1)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(any())).thenAnswer(invocation -> invocation.<Incident>getArgument(0));

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
                "comment", null, null, null, Collections.emptyList(), now, now);
        when(incidentRepository.findById(1)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(any())).thenAnswer(invocation -> invocation.<Incident>getArgument(0));

        var command = new IncidentStateChangeCommand(IncidentState.DENEGADO, null, "motivo");
        var result = service.changeState(1, command);

        assertThat(result.state()).isEqualTo(IncidentState.DENEGADO);
    }

    @Test
    void changeState_approveWithPermission_shouldSetApproved() {
        var now = LocalDateTime.now();
        var incident = Incident.restore(1, 1, 1, IncidentState.PENDIENTE,
                "comment", null, null, null, Collections.emptyList(), now, now);
        var cmd = new IncidentStateChangeCommand(IncidentState.APROBADO, 5, null);
        when(incidentRepository.findById(1)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(i -> i.getArgument(0));

        var result = service.changeState(1, cmd);

        assertThat(result.state()).isEqualTo(IncidentState.APROBADO);
        assertThat(result.permiso()).isNotNull();
    }

    @Test
    void changeState_deny_shouldSetDenied() {
        var now = LocalDateTime.now();
        var incident = Incident.restore(1, 1, 1, IncidentState.PENDIENTE,
                "comment", null, null, null, Collections.emptyList(), now, now);
        var cmd = new IncidentStateChangeCommand(IncidentState.DENEGADO, null, "Invalid reason");
        when(incidentRepository.findById(1)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(i -> i.getArgument(0));

        var result = service.changeState(1, cmd);

        assertThat(result.state()).isEqualTo(IncidentState.DENEGADO);
    }

    @Test
    void changeState_shouldThrowWhenNotFound() {
        var cmd = new IncidentStateChangeCommand(IncidentState.APROBADO, null, null);
        when(incidentRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeState(999, cmd))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void changeState_shouldThrowWhenInvalidState() {
        var now = LocalDateTime.now();
        var incident = Incident.restore(1, 1, 1, IncidentState.PENDIENTE,
                "comment", null, null, null, Collections.emptyList(), now, now);
        var cmd = new IncidentStateChangeCommand(IncidentState.PENDIENTE, null, null);
        when(incidentRepository.findById(1)).thenReturn(Optional.of(incident));

        assertThatThrownBy(() -> service.changeState(1, cmd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Estado no válido");
    }

    @Test
    void changeState_approveWithZeroDays_setsApprovedWithoutPermission() {
        var now = LocalDateTime.now();
        var incident = Incident.restore(1, 1, 1, IncidentState.PENDIENTE,
                "comment", null, null, null, Collections.emptyList(), now, now);
        var cmd = new IncidentStateChangeCommand(IncidentState.APROBADO, 0, null);
        when(incidentRepository.findById(1)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(i -> i.getArgument(0));

        var result = service.changeState(1, cmd);

        assertThat(result.state()).isEqualTo(IncidentState.APROBADO);
        assertThat(result.permiso()).isNull();
    }

    @Test
    void toResult_includesTenantUserInfo() {
        var now = LocalDateTime.now();
        var incident = Incident.restore(1, 123, 1, IncidentState.PENDIENTE,
                "comment", null, null, null, Collections.emptyList(), now, now);
        when(incidentRepository.findById(1)).thenReturn(Optional.of(incident));
        when(tenantUserPort.findBasicInfoById(123L)).thenReturn(
                Optional.of(new TenantUserPort.TenantUserBasicInfo(
                        123L, "Juan", "Perez", "Lopez", "juan@test.com", "5551234")));

        var result = service.findById(1);

        assertThat(result).isPresent();
        assertThat(result.get().tenantUser()).isNotNull();
        assertThat(result.get().tenantUser().nombre()).isEqualTo("Juan");
    }

    @Test
    void toResult_handlesInvalidTenantUserId() {
        var now = LocalDateTime.now();
        var incident = Incident.restore(1, -1, 1, IncidentState.PENDIENTE,
                "comment", null, null, null, Collections.emptyList(), now, now);
        when(incidentRepository.findById(1)).thenReturn(Optional.of(incident));

        var result = service.findById(1);

        assertThat(result).isPresent();
        assertThat(result.get().tenantUser()).isNull();
    }

    @Test
    void toResult_includesEvidenceWithDownloadUrl() {
        var now = LocalDateTime.now();
        var incident = Incident.restore(1, 1, 1, IncidentState.PENDIENTE,
                "comment", null, null, null, Collections.emptyList(), now, now);
        incident.addEvidence(IncidentEvidence.create(1, "file.pdf", "key-1", "application/pdf", 1024));

        when(incidentRepository.findById(1)).thenReturn(Optional.of(incident));

        var result = service.findById(1);

        assertThat(result).isPresent();
        assertThat(result.get().evidencias()).hasSize(1);
        assertThat(result.get().evidencias().get(0).downloadUrl())
                .contains("/evidencias/");
    }
}
