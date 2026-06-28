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
import trazzo.back.incidents.application.dto.command.CreateIncidentTypeCommand;
import trazzo.back.incidents.application.dto.command.PatchIncidentTypeCommand;
import trazzo.back.incidents.application.dto.result.IncidentTypeResult;
import trazzo.back.incidents.application.dto.result.PaginatedResult;
import trazzo.back.incidents.application.port.in.IncidentTypeUseCase;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ExtendWith(SpringExtension.class)
@WebMvcTest(IncidentTypeController.class)
@WithMockUser
class IncidentTypeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IncidentTypeUseCase useCase;

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private IncidentTypeResult sampleResult;

    @BeforeEach
    void setUp() {
        var now = LocalDateTime.now();
        sampleResult = new IncidentTypeResult("id-1", "Permiso", "Desc", true, now, now);
    }

    @Test
    void listReturns200() throws Exception {
        var paginated = new PaginatedResult<>(List.of(sampleResult), 0, 20, 1, 1);
        when(useCase.findAll(null, 0, 20)).thenReturn(paginated);

        mockMvc.perform(get("/incidentes/tipos")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nombre").value("Permiso"));
    }

    @Test
    void createReturns201() throws Exception {
        when(useCase.create(any())).thenReturn(sampleResult);

        var request = new trazzo.back.incidents.infrastructure.adapters.in.web.dto.CreateIncidentTypeRequest("Permiso", "Desc");
        mockMvc.perform(post("/incidentes/tipos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Permiso"));
    }

    @Test
    void createWithBlankNombreReturns422() throws Exception {
        var request = new trazzo.back.incidents.infrastructure.adapters.in.web.dto.CreateIncidentTypeRequest(" ", "Desc");
        mockMvc.perform(post("/incidentes/tipos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void getByIdReturns200() throws Exception {
        when(useCase.findById("id-1")).thenReturn(Optional.of(sampleResult));

        mockMvc.perform(get("/incidentes/tipos/id-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("id-1"));
    }

    @Test
    void getByIdReturns404() throws Exception {
        when(useCase.findById("not-found")).thenReturn(Optional.empty());

        mockMvc.perform(get("/incidentes/tipos/not-found"))
                .andExpect(status().isNotFound());
    }

    @Test
    void patchReturns200() throws Exception {
        when(useCase.patch(anyString(), any())).thenReturn(sampleResult);

        var request = new trazzo.back.incidents.infrastructure.adapters.in.web.dto.PatchIncidentTypeRequest("Nuevo", null, false);
        mockMvc.perform(patch("/incidentes/tipos/id-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Permiso"));
    }
}
