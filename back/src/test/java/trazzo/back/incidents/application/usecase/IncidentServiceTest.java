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

    @Mock
    private EvidenceUrlResolver evidenceUrlResolver;

    @InjectMocks
    private IncidentService service;

    private IncidentType sampleType() {
        return IncidentType.restore("type-1", "Retiro", "Desc", true,
                LocalDateTime.now(), LocalDateTime.now());
    }

    private Incident sampleIncident() {
        return Incident.restore("inc-1", "user-1", "type-1",
                IncidentState.PENDIENTE, "comment", null, null, null,
                Collections.emptyList(), LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void create_shouldReturnResult() {
        var cmd = new CreateIncidentCommand("user-1", "type-1", "Need time off");
        when(typeRepository.findById("type-1")).thenReturn(Optional.of(sampleType()));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(i -> i.getArgument(0));

        var result = service.create(cmd);

        assertThat(result.tenantUserId()).isEqualTo("user-1");
        assertThat(result.state()).isEqualTo(IncidentState.PENDIENTE);
    }

    @Test
    void create_shouldThrowWhenTypeNotFound() {
        var cmd = new CreateIncidentCommand("user-1", "bad-type", "comment");
        when(typeRepository.findById("bad-type")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(cmd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tipo de incidencia no encontrado");
    }

    @Test
    void create_shouldThrowWhenTypeInactive() {
        var inactiveType = IncidentType.restore("type-1", "Retiro", "Desc", false,
                LocalDateTime.now(), LocalDateTime.now());
        var cmd = new CreateIncidentCommand("user-1", "type-1", "comment");
        when(typeRepository.findById("type-1")).thenReturn(Optional.of(inactiveType));

        assertThatThrownBy(() -> service.create(cmd))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no está activo");
    }

    @Test
    void findById_shouldReturnResult() {
        when(incidentRepository.findById("inc-1")).thenReturn(Optional.of(sampleIncident()));

        var result = service.findById("inc-1");

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo("inc-1");
    }

    @Test
    void findAll_shouldReturnPaginatedResult() {
        when(incidentRepository.findAll(null, null, null, null, null, null, 0, 10, null))
                .thenReturn(List.of(sampleIncident()));
        when(incidentRepository.count(null, null, null, null, null, null)).thenReturn(1L);
        when(typeRepository.findByIdIn(anyList())).thenReturn(List.of(sampleType()));

        var result = service.findAll(null, null, null, null, null, null,
                null, null, null, 0, 10, null);

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void patch_shouldUpdateComment() {
        var cmd = new PatchIncidentCommand("Updated comment");
        when(incidentRepository.findById("inc-1")).thenReturn(Optional.of(sampleIncident()));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(i -> i.getArgument(0));

        var result = service.patch("inc-1", cmd);

        assertThat(result.comment()).isEqualTo("Updated comment");
    }

    @Test
    void patch_shouldThrowWhenNotFound() {
        var cmd = new PatchIncidentCommand("comment");
        when(incidentRepository.findById("bad-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.patch("bad-id", cmd))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void changeState_approve_shouldSetApproved() {
        var cmd = new IncidentStateChangeCommand(IncidentState.APROBADO, null, null);
        when(incidentRepository.findById("inc-1")).thenReturn(Optional.of(sampleIncident()));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(i -> i.getArgument(0));

        var result = service.changeState("inc-1", cmd);

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
    void findAll_attachTypes_withBatchAndFallback() {
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
        // Fallback for missing type
        when(typeRepository.findById("type-missing"))
                .thenReturn(Optional.of(IncidentType.restore("type-missing", "Permiso", "Desc", true,
                        LocalDateTime.now(), LocalDateTime.now())));

        var result = service.findAll(null, null, null, null, null, null,
                null, null, null, 0, 10, null);

        assertThat(result.content()).hasSize(2);
        verify(typeRepository).findByIdIn(anyList());
        verify(typeRepository).findById("type-missing");
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
