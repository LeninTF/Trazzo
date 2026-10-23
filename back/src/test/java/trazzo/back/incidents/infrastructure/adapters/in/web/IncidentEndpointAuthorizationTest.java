package trazzo.back.incidents.infrastructure.adapters.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import trazzo.back.corehr.application.port.out.TenantUserPort;
import trazzo.back.incidents.application.dto.result.IncidentTypeResult;
import trazzo.back.incidents.application.dto.result.PaginatedResult;
import trazzo.back.incidents.application.port.in.EvidenceUseCase;
import trazzo.back.incidents.application.port.in.IncidentTypeUseCase;
import trazzo.back.incidents.application.port.in.IncidentUseCase;
import trazzo.back.incidents.application.port.in.NotificationUseCase;
import trazzo.back.incidents.domain.model.IncidentState;
import trazzo.back.incidents.infrastructure.adapters.in.web.dto.CreateIncidentTypeRequest;
import trazzo.back.incidents.infrastructure.adapters.in.web.dto.IncidentStateChangeRequest;
import trazzo.back.incidents.infrastructure.adapters.in.web.dto.NotifyIncidentRequest;
import trazzo.back.incidents.infrastructure.adapters.in.web.dto.PatchIncidentTypeRequest;
import trazzo.back.shared.application.port.out.FileStoragePort;
import trazzo.back.shared.security.TenantSecuritySlice;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static trazzo.back.shared.security.TenantSecurityContexts.actingAsPlainUser;
import static trazzo.back.shared.security.TenantSecurityContexts.actingAsTenantUserWith;

/** Matriz permiso → endpoint de incidencias (AUD-05, AUD-08). */
@WebMvcTest({IncidentController.class, IncidentTypeController.class})
@AutoConfigureMockMvc(addFilters = false)
@TenantSecuritySlice
class IncidentEndpointAuthorizationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    IncidentUseCase incidentUseCase;
    @MockitoBean
    EvidenceUseCase evidenceUseCase;
    @MockitoBean
    NotificationUseCase notificationUseCase;
    @MockitoBean
    FileStoragePort fileStoragePort;
    @MockitoBean
    TenantUserPort tenantUserPort;
    @MockitoBean
    IncidentTypeUseCase incidentTypeUseCase;

    private String json(Object body) throws Exception {
        return mapper.writeValueAsString(body);
    }

    @Test
    void thePlainUserCannotResolveIncidentsNorManageTheirTypes() throws Exception {
        actingAsPlainUser();

        mockMvc.perform(patch("/incidentes/1/estado").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new IncidentStateChangeRequest(IncidentState.APROBADO, null, null))))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/incidentes/1/notificar").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new NotifyIncidentRequest("JUSTIFICACION"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/incidentes/1/justificar")).andExpect(status().isForbidden());
        mockMvc.perform(post("/incidentes/tipos").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CreateIncidentTypeRequest("Permiso", "Desc"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/incidentes/tipos/1").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new PatchIncidentTypeRequest("Nuevo", null, false))))
                .andExpect(status().isForbidden());
        verifyNoInteractions(incidentUseCase, notificationUseCase, incidentTypeUseCase);
    }

    @Test
    void everyoneSeesTheIncidentTypes() throws Exception {
        actingAsPlainUser();
        when(incidentTypeUseCase.findAll(any(), anyInt(), anyInt())).thenReturn(new PaginatedResult<>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/incidentes/tipos")).andExpect(status().isOk());
    }

    @Test
    void withoutIncidentPermissionsThereAreNoIncidents() throws Exception {
        actingAsTenantUserWith("asistencia-docente.ver-historial", "horarios-docente.ver-turnos");

        mockMvc.perform(get("/incidentes")).andExpect(status().isForbidden());
        mockMvc.perform(get("/incidentes/1")).andExpect(status().isForbidden());
        verifyNoInteractions(incidentUseCase);
    }

    @Test
    void anApproverSeesIncidentsWithoutTheOwnIncidentsPermission() throws Exception {
        actingAsTenantUserWith("permisos-licencias.aprobar-incidencias");
        when(incidentUseCase.findAll(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), any()))
                .thenReturn(new PaginatedResult<>(List.of(), 0, 10, 0, 0));

        mockMvc.perform(get("/incidentes")).andExpect(status().isOk());
    }

    @Test
    void attendanceJustifiersCanJustifyAnIncident() throws Exception {
        actingAsTenantUserWith("control-asistencia.justificar-marcaciones");

        mockMvc.perform(post("/incidentes/1/justificar")).andExpect(status().isAccepted());
        verify(notificationUseCase).justifyAttendance(1);
    }

    @Test
    void leaveApproversManageTheIncidentTypes() throws Exception {
        actingAsTenantUserWith("permisos-licencias.aprobar-incidencias");
        var now = LocalDateTime.now();
        when(incidentTypeUseCase.patch(eq(1), any())).thenReturn(new IncidentTypeResult(1, "Nuevo", "Desc", false, now, now));

        mockMvc.perform(patch("/incidentes/tipos/1").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new PatchIncidentTypeRequest("Nuevo", null, false))))
                .andExpect(status().isOk());
    }
}
