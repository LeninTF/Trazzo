package trazzo.back.corehr.infrastructure.adapters.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import trazzo.back.corehr.application.dto.command.CreateToleranciaCommand;
import trazzo.back.corehr.application.dto.command.PatchToleranciaCommand;
import trazzo.back.corehr.application.dto.result.PaginatedResult;
import trazzo.back.corehr.application.dto.result.ToleranciaResult;
import trazzo.back.corehr.application.port.in.ToleranciaUseCase;
import trazzo.back.corehr.domain.exception.InvalidToleranciaException;
import trazzo.back.corehr.domain.model.ToleranciaType;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ToleranciaController.class)
@AutoConfigureMockMvc(addFilters = false)
class ToleranciaControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ToleranciaUseCase toleranciaUseCase;

    private static final LocalDateTime NOW = LocalDateTime.now();

    private static ToleranciaResult aResult() {
        return new ToleranciaResult(1L, 1L, "T1", ToleranciaType.ENTRADA, 15, "desc", true, NOW, NOW);
    }

    @Test
    void listBySchedule_shouldReturn200() throws Exception {
        var paginated = new PaginatedResult<ToleranciaResult>(List.of(aResult()), 0, 20, 1, 1);
        when(toleranciaUseCase.findAllByScheduleId(eq(1L), anyInt(), anyInt())).thenReturn(paginated);

        mockMvc.perform(get("/corehr/schedules/1/tolerancias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("T1"))
                .andExpect(jsonPath("$.content[0].type").value("ENTRADA"))
                .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    void listBySchedule_shouldRespectPagination() throws Exception {
        var paginated = new PaginatedResult<ToleranciaResult>(List.of(aResult()), 1, 5, 1, 1);
        when(toleranciaUseCase.findAllByScheduleId(eq(1L), eq(1), eq(5))).thenReturn(paginated);

        mockMvc.perform(get("/corehr/schedules/1/tolerancias")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5));
    }

    @Test
    void create_shouldReturn201() throws Exception {
        when(toleranciaUseCase.create(anyLong(), any(CreateToleranciaCommand.class))).thenReturn(aResult());

        mockMvc.perform(post("/corehr/schedules/1/tolerancias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "ENTRADA", "minutes": 15, "name": "T1"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("T1"));
    }

    @Test
    void create_shouldReturn400WhenTypeIsNull() throws Exception {
        mockMvc.perform(post("/corehr/schedules/1/tolerancias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"minutes": 15}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_shouldReturn400WhenMinutesIsNull() throws Exception {
        mockMvc.perform(post("/corehr/schedules/1/tolerancias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "ENTRADA"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_shouldReturn400WhenMinutesExceedsMax() throws Exception {
        mockMvc.perform(post("/corehr/schedules/1/tolerancias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "ENTRADA", "minutes": 99}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_shouldReturn422WhenUseCaseThrowsInvalidToleranciaException() throws Exception {
        when(toleranciaUseCase.create(anyLong(), any(CreateToleranciaCommand.class)))
                .thenThrow(new InvalidToleranciaException("Tolerancia invalid"));

        mockMvc.perform(post("/corehr/schedules/1/tolerancias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "ENTRADA", "minutes": 15}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Unprocessable Entity"))
                .andExpect(jsonPath("$.message").value("Tolerancia invalid"));
    }

    @Test
    void patch_shouldReturn200() throws Exception {
        when(toleranciaUseCase.patch(anyLong(), anyLong(), any(PatchToleranciaCommand.class)))
                .thenReturn(aResult());

        mockMvc.perform(patch("/corehr/schedules/1/tolerancias/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Updated"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("T1"));
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        doNothing().when(toleranciaUseCase).deleteById(1L, 1L);

        mockMvc.perform(delete("/corehr/schedules/1/tolerancias/1"))
                .andExpect(status().isNoContent());
    }
}
