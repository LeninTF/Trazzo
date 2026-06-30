package trazzo.back.incidents.infrastructure.adapters.in.web;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import trazzo.back.incidents.application.dto.command.*;
import trazzo.back.incidents.application.dto.result.*;
import trazzo.back.incidents.application.port.in.EvidenceUseCase;
import trazzo.back.incidents.application.port.in.IncidentUseCase;
import trazzo.back.incidents.application.port.in.NotificationUseCase;
import trazzo.back.incidents.domain.model.IncidentState;
import trazzo.back.incidents.infrastructure.adapters.in.web.dto.*;
import trazzo.back.shared.application.port.out.FileStoragePort;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ExtendWith(SpringExtension.class)
@WebMvcTest(IncidentController.class)
@WithMockUser
class IncidentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IncidentUseCase incidentUseCase;

    @MockitoBean
    private EvidenceUseCase evidenceUseCase;

    @MockitoBean
    private NotificationUseCase notificationUseCase;

    @MockitoBean
    private FileStoragePort fileStoragePort;

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private IncidentResult sampleResult;
    private IncidentEvidenceResult sampleEvidenceResult;

    @BeforeEach
    void setUp() {
        var now = LocalDateTime.now();
        sampleResult = new IncidentResult("inc-1", "u-1", "t-1", IncidentState.PENDIENTE,
                "comment", null, null, null, List.of(), null, now, now);
        sampleEvidenceResult = new IncidentEvidenceResult("ev-1", "inc-1", "doc.pdf",
                "file-key", "http://url", "pdf", 100, now, now);
    }

    @Test
    void listReturns200() throws Exception {
        var paginated = new PaginatedResult<>(List.of(sampleResult), 0, 20, 1, 1);
        when(incidentUseCase.findAll(any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), any()))
                .thenReturn(paginated);

        mockMvc.perform(get("/incidentes")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("inc-1"));
    }

    @Test
    void createReturns201() throws Exception {
        when(incidentUseCase.create(any())).thenReturn(sampleResult);

        var request = new CreateIncidentRequest("u-1", "comment", "t-1");
        mockMvc.perform(post("/incidentes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("inc-1"));
    }

    @Test
    void getByIdReturns200() throws Exception {
        when(incidentUseCase.findById("inc-1")).thenReturn(Optional.of(sampleResult));

        mockMvc.perform(get("/incidentes/inc-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("inc-1"));
    }

    @Test
    void getByIdReturns404() throws Exception {
        when(incidentUseCase.findById("not-found")).thenReturn(Optional.empty());

        mockMvc.perform(get("/incidentes/not-found"))
                .andExpect(status().isNotFound());
    }

    @Test
    void patchReturns200() throws Exception {
        when(incidentUseCase.patch(anyString(), any())).thenReturn(sampleResult);

        var request = new PatchIncidentRequest("nuevo comentario");
        mockMvc.perform(patch("/incidentes/inc-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("inc-1"));
    }

    @Test
    void changeStateReturns200() throws Exception {
        when(incidentUseCase.changeState(anyString(), any())).thenReturn(sampleResult);

        var request = new IncidentStateChangeRequest(IncidentState.APROBADO, null, null);
        mockMvc.perform(patch("/incidentes/inc-1/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("inc-1"));
    }

    @Test
    void createEvidenceReturns201() throws Exception {
        when(evidenceUseCase.create(anyString(), any())).thenReturn(sampleEvidenceResult);

        var request = new CreateEvidenceRequest("doc.pdf", "file-key", "pdf", 100);
        mockMvc.perform(post("/incidentes/inc-1/evidencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.file_name").value("doc.pdf"));
    }

    @Test
    void listEvidencesReturns200() throws Exception {
        when(evidenceUseCase.findAllByIncidentId("inc-1")).thenReturn(List.of(sampleEvidenceResult));

        mockMvc.perform(get("/incidentes/inc-1/evidencias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].file_name").value("doc.pdf"));
    }

    @Test
    void deleteEvidenceReturns204() throws Exception {
        doNothing().when(evidenceUseCase).delete("inc-1", "ev-1");

        mockMvc.perform(delete("/incidentes/inc-1/evidencias/ev-1")
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void notifyReturns202() throws Exception {
        doNothing().when(notificationUseCase).notify(anyString(), any());

        var request = new NotifyIncidentRequest("JUSTIFICACION");
        mockMvc.perform(post("/incidentes/inc-1/notificar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isAccepted());
    }

    @Test
    void justifyReturns202() throws Exception {
        doNothing().when(notificationUseCase).justifyAttendance("inc-1");

        mockMvc.perform(post("/incidentes/inc-1/justificar")
                        .with(csrf()))
                .andExpect(status().isAccepted());
    }
}
