package trazzo.back.corehr.infrastructure.adapters.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import trazzo.back.corehr.application.dto.command.CreateShiftCommand;
import trazzo.back.corehr.application.dto.command.PatchShiftCommand;
import trazzo.back.corehr.application.dto.result.PaginatedResult;
import trazzo.back.corehr.application.dto.result.ShiftResult;
import trazzo.back.corehr.application.port.in.ShiftUseCase;
import trazzo.back.corehr.domain.exception.InvalidShiftException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ShiftController.class)
@AutoConfigureMockMvc(addFilters = false)
class ShiftControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ShiftUseCase shiftUseCase;

    private static final LocalDateTime NOW = LocalDateTime.now();

    private static ShiftResult aResult() {
        return new ShiftResult(1L, "Morning", "desc", List.of(), NOW, NOW);
    }

    @Test
    void list_shouldReturn200() throws Exception {
        var paginated = new PaginatedResult<ShiftResult>(List.of(aResult()), 0, 20, 1, 1);
        when(shiftUseCase.findAll(any(), anyInt(), anyInt(), any())).thenReturn(paginated);

        mockMvc.perform(get("/corehr/shifts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Morning"))
                .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    void list_shouldFilterBySearch() throws Exception {
        var paginated = new PaginatedResult<ShiftResult>(List.of(aResult()), 0, 10, 1, 1);
        when(shiftUseCase.findAll(eq("Morning"), eq(0), eq(10), isNull())).thenReturn(paginated);

        mockMvc.perform(get("/corehr/shifts")
                        .param("search", "Morning")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Morning"));
    }

    @Test
    void create_shouldReturn201() throws Exception {
        when(shiftUseCase.create(any(CreateShiftCommand.class))).thenReturn(aResult());

        mockMvc.perform(post("/corehr/shifts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Morning", "description": "desc"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Morning"));
    }

    @Test
    void create_shouldReturn422WhenUseCaseThrowsInvalidShiftException() throws Exception {
        when(shiftUseCase.create(any(CreateShiftCommand.class)))
                .thenThrow(new InvalidShiftException("Shift conflict"));

        mockMvc.perform(post("/corehr/shifts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Morning"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Unprocessable Entity"))
                .andExpect(jsonPath("$.message").value("Shift conflict"));
    }

    @Test
    void getById_shouldReturn200WhenFound() throws Exception {
        when(shiftUseCase.findById(1L)).thenReturn(Optional.of(aResult()));

        mockMvc.perform(get("/corehr/shifts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getById_shouldReturn404WhenNotFound() throws Exception {
        when(shiftUseCase.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/corehr/shifts/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void patch_shouldReturn200() throws Exception {
        when(shiftUseCase.patch(anyLong(), any(PatchShiftCommand.class))).thenReturn(aResult());

        mockMvc.perform(patch("/corehr/shifts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Updated"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Morning"));
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        doNothing().when(shiftUseCase).deleteById(1L);

        mockMvc.perform(delete("/corehr/shifts/1"))
                .andExpect(status().isNoContent());
    }
}
